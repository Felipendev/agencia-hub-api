package com.agenciahub.api.application.services;

import com.agenciahub.api.domain.MilesWallet.*;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.exception.WalletConflictException;
import com.agenciahub.api.security.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import static com.agenciahub.api.domain.MilesWallet.invalid;

@Service @RequiredArgsConstructor
public class MilesWalletService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private void customer(UUID id, boolean lock) {
        if(jdbc.query("SELECT id FROM customers WHERE id=? AND agency_id=?"+(lock?" FOR UPDATE":""),(r,n)->r.getObject(1,UUID.class),id,TenantContext.requireAgencyId()).isEmpty())
            throw new ResourceNotFoundException("Pessoa não encontrada.");
    }
    @Transactional(readOnly=true)
    public View view(UUID customer) {
        customer(customer,false);
        return new View(profile(customer),items(customer),movements(customer),jdbc.query("SELECT * FROM customer_miles_audit WHERE customer_id=? AND agency_id=? ORDER BY created_at DESC,id",(r,n)->
            new Audit(r.getObject("id",UUID.class),r.getObject("resource_id",UUID.class),r.getString("action"),read(r.getString("snapshot"),Object.class),r.getString("author"),r.getObject("created_at",OffsetDateTime.class)),customer,TenantContext.requireAgencyId()));
    }
    private ProfileRow profile(UUID customer) {
        return jdbc.query("SELECT * FROM customer_miles_profiles WHERE customer_id=? AND agency_id=?",(r,n)->new ProfileRow(read(r.getString("data"),Profile.class),r.getInt("version"),r.getBoolean("archived")),customer,TenantContext.requireAgencyId()).stream().findFirst().orElse(null);
    }
    private List<Item> items(UUID customer) {
        return jdbc.query("SELECT i.*,COALESCE((SELECT SUM(m.quantity) FROM customer_miles_movements m WHERE m.account_id=i.id),0) AS balance FROM customer_miles_items i WHERE customer_id=? AND agency_id=? ORDER BY updated_at DESC,id",(r,n)->
            new Item(r.getObject("id",UUID.class),Kind.valueOf(r.getString("kind")),read(r.getString("data"),Data.class),r.getInt("version"),r.getBoolean("archived"),r.getLong("balance")),customer,TenantContext.requireAgencyId());
    }
    private Item item(UUID customer,UUID id) { return items(customer).stream().filter(i->i.id().equals(id)).findFirst().orElseThrow(()->new ResourceNotFoundException("Registro da carteira não encontrado.")); }
    private List<Movement> movements(UUID customer) {
        return jdbc.query("SELECT m.*,EXISTS(SELECT 1 FROM customer_miles_movements x WHERE x.reverses_id=m.id) AS reversed FROM customer_miles_movements m JOIN customer_miles_items i ON i.id=m.account_id WHERE i.customer_id=? AND i.agency_id=? ORDER BY m.created_at DESC,m.id",(r,n)->
            new Movement(r.getObject("id",UUID.class),r.getObject("account_id",UUID.class),r.getLong("quantity"),r.getString("reason"),r.getObject("occurred_on",LocalDate.class),r.getObject("reverses_id",UUID.class),r.getString("author"),r.getObject("created_at",OffsetDateTime.class),r.getBoolean("reversed")),customer,TenantContext.requireAgencyId());
    }
    @Transactional
    public ProfileRow saveProfile(UUID customer,ProfileInput input,String author) {
        customer(customer,true); ProfileRow before=profile(customer);
        if(before!=null&&!before.archived()&&before.data().equals(input.data()))return before;
        version(before==null?0:before.version(),input.expectedVersion());
        if(before==null)jdbc.update("INSERT INTO customer_miles_profiles(customer_id,agency_id,data) VALUES(?,?,?::jsonb)",customer,TenantContext.requireAgencyId(),write(input.data()));
        else jdbc.update("UPDATE customer_miles_profiles SET data=?::jsonb,version=version+1,archived=false,updated_at=now() WHERE customer_id=? AND agency_id=?",write(input.data()),customer,TenantContext.requireAgencyId());
        ProfileRow row=profile(customer);audit(customer,customer,"PROFILE_SAVED",row,author);return row;
    }
    @Transactional
    public void archiveProfile(UUID customer,int expectedVersion,String author) {
        customer(customer,true);ProfileRow row=profile(customer);if(row==null)throw new ResourceNotFoundException("Perfil não encontrado.");
        if(row.archived())return;version(row.version(),expectedVersion);
        jdbc.update("UPDATE customer_miles_profiles SET archived=true,version=version+1,updated_at=now() WHERE customer_id=? AND agency_id=?",customer,TenantContext.requireAgencyId());
        audit(customer,customer,"PROFILE_ARCHIVED",profile(customer),author);
    }
    @Transactional
    public Item create(UUID customer,UUID requestKey,ItemInput input,String author) {
        customer(customer,true);input.data().validate(input.kind());validateReference(customer,input.kind(),input.data());
        Item existing=items(customer).stream().filter(i->i.id().equals(requestKey)).findFirst().orElse(null);
        if(existing!=null){if(existing.kind()!=input.kind()||!existing.data().equals(input.data()))conflict("A confirmação já foi utilizada para outro cadastro.");return existing;}
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM customer_miles_audit WHERE customer_id=? AND agency_id=? AND resource_id=? AND action='ITEM_CREATED')",Boolean.class,customer,TenantContext.requireAgencyId(),requestKey)))
            conflict("Este cadastro já foi excluído. Recarregue a carteira antes de criar outro registro.");
        jdbc.update("INSERT INTO customer_miles_items(id,customer_id,agency_id,kind,data) VALUES(?,?,?,?,?::jsonb)",requestKey,customer,TenantContext.requireAgencyId(),input.kind().name(),write(input.data()));
        if(input.kind()==Kind.ACCOUNT&&input.data().openingBalance()>0)insertMovement(requestKey,requestKey,input.data().openingBalance(),"Saldo inicial declarado",input.data().declaredOn(),null,author);
        Item row=item(customer,requestKey);audit(customer,row.id(),"ITEM_CREATED",row,author);return row;
    }
    @Transactional
    public Item update(UUID customer,UUID id,ItemInput input,String author) {
        customer(customer,true);Item before=item(customer,id);active(before);input.data().validate(input.kind());
        if(before.kind()!=input.kind())invalid("O tipo do registro não pode ser alterado.");
        if(before.kind()==Kind.ACCOUNT && (!before.data().openingBalance().equals(input.data().openingBalance())||!before.data().declaredOn().equals(input.data().declaredOn())||!before.data().source().equals(input.data().source())))
            invalid("Saldo inicial, fonte e data são históricos. Use uma movimentação para ajustar o saldo.");
        validateReference(customer,input.kind(),input.data());
        if(before.data().equals(input.data()))return before;
        version(before.version(),input.expectedVersion());
        jdbc.update("UPDATE customer_miles_items SET data=?::jsonb,version=version+1,updated_at=now() WHERE id=? AND customer_id=? AND agency_id=?",write(input.data()),id,customer,TenantContext.requireAgencyId());
        Item row=item(customer,id);audit(customer,id,"ITEM_UPDATED",row,author);return row;
    }
    @Transactional
    public void remove(UUID customer,UUID id,int expectedVersion,String author) {
        customer(customer,true);Item row=item(customer,id);if(row.archived())return;version(row.version(),expectedVersion);
        if(row.kind()==Kind.ACCOUNT)jdbc.update("UPDATE customer_miles_items SET archived=true,version=version+1,updated_at=now() WHERE id=? AND customer_id=? AND agency_id=?",id,customer,TenantContext.requireAgencyId());
        else jdbc.update("DELETE FROM customer_miles_items WHERE id=? AND customer_id=? AND agency_id=?",id,customer,TenantContext.requireAgencyId());
        audit(customer,id,row.kind()==Kind.ACCOUNT?"ACCOUNT_ARCHIVED":"ITEM_DELETED",row,author);
    }
    @Transactional
    public Item restore(UUID customer,UUID id,int expectedVersion,String author) {
        customer(customer,true);Item row=item(customer,id);if(row.kind()!=Kind.ACCOUNT)invalid("Somente contas são arquivadas.");if(!row.archived())return row;version(row.version(),expectedVersion);
        jdbc.update("UPDATE customer_miles_items SET archived=false,version=version+1,updated_at=now() WHERE id=? AND customer_id=? AND agency_id=?",id,customer,TenantContext.requireAgencyId());
        Item restored=item(customer,id);audit(customer,id,"ACCOUNT_RESTORED",restored,author);return restored;
    }
    @Transactional
    public Movement addMovement(UUID customer,UUID account,MovementInput input,String author) {
        customer(customer,true);Item row=item(customer,account);
        if(row.kind()!=Kind.ACCOUNT)invalid("Selecione uma conta de programa.");
        UUID existing=jdbc.query("SELECT id FROM customer_miles_movements WHERE account_id=? AND request_key=?",(r,n)->r.getObject(1,UUID.class),account,input.requestKey()).stream().findFirst().orElse(null);
        if(existing!=null){Movement old=movement(customer,existing);if(old.quantity()!=input.quantity()||!old.reason().equals(input.reason())||!old.occurredOn().equals(input.occurredOn()))conflict("A confirmação já foi utilizada para outra movimentação.");return old;}
        active(row);if(input.quantity()==0)invalid("A quantidade deve ser diferente de zero.");
        if(row.balance()+input.quantity()<0)conflict("A saída excede o saldo declarado disponível.");
        UUID id=UUID.randomUUID();insertMovement(id,account,input.quantity(),input.reason(),input.occurredOn(),null,author,input.requestKey());
        Movement move=movement(customer,id);audit(customer,id,"MOVEMENT_CREATED",move,author);return move;
    }
    @Transactional
    public Movement reverse(UUID customer,UUID id,String reason,String author) {
        customer(customer,true);Movement original=movement(customer,id);
        if(original.reversesId()!=null)invalid("Um estorno não pode ser estornado; registre uma nova movimentação.");
        Movement existing=movements(customer).stream().filter(m->id.equals(m.reversesId())).findFirst().orElse(null);if(existing!=null)return existing;
        Item account=item(customer,original.accountId());active(account);
        if(account.balance()-original.quantity()<0)conflict("O estorno deixaria o saldo negativo. Revise as movimentações posteriores.");
        UUID reversal=UUID.randomUUID();insertMovement(reversal,account.id(),-original.quantity(),reason,LocalDate.now(),id,author);
        Movement row=movement(customer,reversal);audit(customer,reversal,"MOVEMENT_REVERSED",row,author);return row;
    }
    private Movement movement(UUID customer,UUID id) {return movements(customer).stream().filter(m->m.id().equals(id)).findFirst().orElseThrow(()->new ResourceNotFoundException("Movimentação não encontrada."));}
    private void insertMovement(UUID id,UUID account,long quantity,String reason,LocalDate date,UUID reverses,String author) {insertMovement(id,account,quantity,reason,date,reverses,author,id);}
    private void insertMovement(UUID id,UUID account,long quantity,String reason,LocalDate date,UUID reverses,String author,UUID key) {
        jdbc.update("INSERT INTO customer_miles_movements(id,account_id,quantity,reason,occurred_on,request_key,reverses_id,author) VALUES(?,?,?,?,?,?,?,?)",id,account,quantity,reason,date,key,reverses,author);
    }
    private void validateReference(UUID customer,Kind kind,Data data) {
        if(kind==Kind.EXPIRY){Item account=item(customer,data.accountId());if(account.kind()!=Kind.ACCOUNT)invalid("Vencimento deve estar vinculado a uma conta.");active(account);}
    }
    private void active(Item row) {if(row.archived())conflict("Reative a conta antes de alterar ou movimentar.");}
    private void version(int actual,int expected) {if(actual!=expected)conflict("O registro foi atualizado por outra pessoa. Recarregue antes de editar.");}
    private void conflict(String message) {throw new WalletConflictException(message);}
    private void audit(UUID customer,UUID resource,String action,Object snapshot,String author) {
        jdbc.update("INSERT INTO customer_miles_audit(id,customer_id,agency_id,resource_id,action,snapshot,author) VALUES(?,?,?,?,?,?::jsonb,?)",UUID.randomUUID(),customer,TenantContext.requireAgencyId(),resource,action,write(snapshot),author);
    }
    private String write(Object value) {try{return mapper.writeValueAsString(value);}catch(java.io.IOException e){throw new IllegalStateException("Não foi possível salvar a carteira.",e);}}
    private <T>T read(String value,Class<T> type) {try{return mapper.readValue(value,type);}catch(java.io.IOException e){throw new IllegalStateException("Não foi possível consultar a carteira.",e);}}
}
