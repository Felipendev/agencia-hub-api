package com.agenciahub.api.application.controller;

import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.URI;
import java.util.*;

@RestController @RequestMapping("/library")
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')") @RequiredArgsConstructor
public class LibraryController {
    private final JdbcTemplate jdbc;
    private static final String SELECT = "SELECT id,title,category,kind,url,body,filename,content_type,byte_size FROM agency_library WHERE agency_id=?";
    private static final Set<String> CATEGORIES = Set.of("BAGAGEM","IMIGRACAO","FORMULARIO","CONTRATO","AGENCIA","NOTA_FISCAL","OUTROS");
    private static final Set<String> MIME = Set.of("application/pdf","text/plain","text/csv","image/jpeg","image/png","image/webp","application/msword","application/vnd.openxmlformats-officedocument.wordprocessingml.document","application/vnd.ms-excel","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    @GetMapping @Transactional public List<Resource> list() {
        UUID agencyId=TenantContext.requireAgencyId();
        // The marker survives deleting defaults, so removed links never reappear.
        if(jdbc.update("INSERT INTO agency_library_initialized(agency_id) VALUES(?) ON CONFLICT DO NOTHING",agencyId)==1) {
            String[][] defaults={
                {"LATAM — bagagem","BAGAGEM","https://www.latamairlines.com/br/pt/central-ajuda/perguntas/bagagem"},
                {"GOL — bagagem","BAGAGEM","https://www.voegol.com.br/pt-br/informacoes/bagagem"},
                {"Azul — bagagem","BAGAGEM","https://www.voeazul.com.br/pt/pt/sua-viagem/planeje/bagagens"},
                {"Portal consular — vistos e documentos","IMIGRACAO","https://www.gov.br/mre/pt-br/assuntos/portal-consular"}
            };
            for(String[] link:defaults) jdbc.update("INSERT INTO agency_library(id,agency_id,title,category,kind,url) SELECT ?,?,?,?,'LINK',? WHERE NOT EXISTS (SELECT 1 FROM agency_library WHERE agency_id=? AND kind='LINK' AND url=?)",UUID.randomUUID(),agencyId,link[0],link[1],link[2],agencyId,link[2]);
        }
        return jdbc.query(SELECT + " ORDER BY lower(title),id", (rs, n) -> new Resource(rs.getObject("id", UUID.class),rs.getString("title"),rs.getString("category"),rs.getString("kind"),rs.getString("url"),rs.getString("body"),rs.getString("filename"),rs.getString("content_type"),rs.getObject("byte_size",Long.class)),agencyId);
    }
    private Resource find(UUID id) {
        return jdbc.query(SELECT+" AND id=?",(rs,n)->new Resource(rs.getObject("id",UUID.class),rs.getString("title"),rs.getString("category"),rs.getString("kind"),rs.getString("url"),rs.getString("body"),rs.getString("filename"),rs.getString("content_type"),rs.getObject("byte_size",Long.class)),TenantContext.requireAgencyId(),id)
          .stream().findFirst().orElseThrow(()->new ResourceNotFoundException("material não encontrado"));
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
    public Resource create(@Valid @RequestBody Request r) {
        validate(r,false); UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO agency_library(id,agency_id,title,category,kind,url,body) VALUES(?,?,?,?,?,?,?)",id,TenantContext.requireAgencyId(),r.title().strip(),r.category(),r.kind(),"LINK".equals(r.kind())?r.url().strip():null,"TEMPLATE".equals(r.kind())?r.body():null);
        return find(id);
    }
    @PatchMapping("/{id}") @Transactional
    public Resource update(@PathVariable UUID id,@Valid @RequestBody Request r) {
        Resource previous=find(id); validate(r,"FILE".equals(previous.kind()));
        if (!previous.kind().equals(r.kind())) throw new IllegalArgumentException("Mantenha o tipo do material ao editar.");
        jdbc.update("UPDATE agency_library SET title=?,category=?,url=?,body=?,updated_at=now() WHERE id=? AND agency_id=?",r.title().strip(),r.category(),"LINK".equals(r.kind())?r.url().strip():null,"TEMPLATE".equals(r.kind())?r.body():null,id,TenantContext.requireAgencyId()); return find(id);
    }
    @PostMapping(value="/files",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) @Transactional
    public Resource upload(@RequestParam String title,@RequestParam String category,@RequestPart("file") MultipartFile file) throws IOException {
        validate(new Request(title,category,"FILE",null,null),true);
        if(file.isEmpty()||file.getSize()>5L*1024*1024||!MIME.contains(Objects.toString(file.getContentType(),""))) throw new IllegalArgumentException("Envie PDF, imagem, Word, Excel ou texto de até 5 MB.");
        String filename=Objects.toString(file.getOriginalFilename(),"arquivo").replaceAll("[\\r\\n\\\\/]","_");
        if(filename.length()>255) filename=filename.substring(filename.length()-255);
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO agency_library(id,agency_id,title,category,kind,filename,content_type,byte_size,content) VALUES(?,?,?,?,?,?,?,?,?)",id,TenantContext.requireAgencyId(),title.strip(),category,"FILE",filename,file.getContentType(),file.getSize(),file.getBytes()); return find(id);
    }
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        Resource r=find(id); if(!"FILE".equals(r.kind())) throw new IllegalArgumentException("Este material não é um arquivo.");
        byte[] bytes=jdbc.queryForObject("SELECT content FROM agency_library WHERE id=? AND agency_id=?",byte[].class,id,TenantContext.requireAgencyId());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(r.contentType())).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(r.filename(),java.nio.charset.StandardCharsets.UTF_8).build().toString()).body(bytes);
    }
    @PutMapping(value="/{id}/file",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @Transactional
    public Resource replaceFile(@PathVariable UUID id,@RequestParam String title,@RequestParam String category,@RequestPart("file") MultipartFile file) throws IOException {
        Resource previous=find(id);
        if(!"FILE".equals(previous.kind())) throw new IllegalArgumentException("Este material não é um arquivo.");
        validate(new Request(title,category,"FILE",null,null),true);
        if(file.isEmpty()||file.getSize()>5L*1024*1024||!MIME.contains(Objects.toString(file.getContentType(),""))) throw new IllegalArgumentException("Envie PDF, imagem, Word, Excel ou texto de até 5 MB.");
        String filename=Objects.toString(file.getOriginalFilename(),"arquivo").replaceAll("[\\r\\n\\\\/]","_");
        if(filename.length()>255) filename=filename.substring(filename.length()-255);
        jdbc.update("UPDATE agency_library SET title=?,category=?,filename=?,content_type=?,byte_size=?,content=?,updated_at=now() WHERE id=? AND agency_id=?",title.strip(),category,filename,file.getContentType(),file.getSize(),file.getBytes(),id,TenantContext.requireAgencyId());
        return find(id);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id) { find(id); jdbc.update("DELETE FROM agency_library WHERE id=? AND agency_id=?",id,TenantContext.requireAgencyId()); }
    static void validate(Request r,boolean fileAllowed) {
        if(r.title()==null||r.title().isBlank()||r.title().length()>200||!CATEGORIES.contains(Objects.toString(r.category(),""))) throw new IllegalArgumentException("Informe título e categoria válidos.");
        if(!Set.of("LINK","TEMPLATE","FILE").contains(Objects.toString(r.kind(),""))||"FILE".equals(r.kind())&&!fileAllowed) throw new IllegalArgumentException("Tipo de material inválido.");
        if("LINK".equals(r.kind())) {
            try { URI uri=URI.create(Objects.toString(r.url(),"").strip()); if(!Set.of("http","https").contains(Objects.toString(uri.getScheme(),"").toLowerCase())||uri.getHost()==null||uri.getUserInfo()!=null||r.url().length()>2048) throw new IllegalArgumentException(); }
            catch(RuntimeException e) { throw new IllegalArgumentException("Informe um link completo começando com https:// ou http://."); }
        }
        if("TEMPLATE".equals(r.kind())&&(r.body()==null||r.body().isBlank()||r.body().length()>20000)) throw new IllegalArgumentException("Informe um modelo de até 20.000 caracteres.");
    }
    public record Request(@NotBlank @Size(max=200) String title,String category,String kind,@Size(max=2048) String url,@Size(max=20000) String body) {}
    public record Resource(UUID id,String title,String category,String kind,String url,String body,String filename,String contentType,Long byteSize) {}
}
