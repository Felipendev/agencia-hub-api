-- Recover the selected option's cost without summing alternative offers.
CREATE TEMP TABLE quotation_cost_repair ON COMMIT DROP AS
SELECT s.id sale_id, s.agency_id, s.customer_id, s.total_amount, s.status,
       q.id quotation_id, q.title,
       ((o->'calculo'->>'basePorPessoa')::numeric * (o->>'qtdPessoas')::integer
         + COALESCE((o->>'precoBagagens')::numeric,0))::numeric(19,2) cost,
       NULLIF(o->'segmentos'->0->>'departureDate','')::date start_date,
       COALESCE(NULLIF(o->'segmentos'->-1->>'arrivalDate',''),NULLIF(o->'segmentos'->-1->>'departureDate',''))::date end_date
FROM sales s JOIN quotations q ON q.id=s.quotation_id
CROSS JOIN LATERAL jsonb_array_elements(q.flight_plan->'options') o
WHERE s.approval_managed AND q.status='ACCEPTED' AND s.status<>'CANCELLED'
  AND o->>'id'=q.flight_plan->>'selectedOptionId'
  AND o->'calculo'->>'basePorPessoa' IS NOT NULL
  AND (SELECT count(*) FROM sale_items i WHERE i.sale_id=s.id)=1;

UPDATE sale_items i SET supplier_cost=c.cost
FROM quotation_cost_repair c WHERE i.sale_id=c.sale_id AND i.supplier_cost IS NULL;

-- Enrich already received income without changing paid amounts or dates.
UPDATE financial_entries f SET sale_amount=r.amount,
  supplier_cost=round(c.cost*r.amount/c.total_amount,2), commission_amount=0
FROM receivables r JOIN quotation_cost_repair c ON c.sale_id=r.sale_id
WHERE f.id=r.financial_entry_id AND c.total_amount>0 AND f.supplier_cost IS NULL;

WITH planned AS MATERIALIZED (
  SELECT r.id receivable_id,gen_random_uuid() entry_id,r.amount,r.due_date,c.*
  FROM receivables r JOIN quotation_cost_repair c ON c.sale_id=r.sale_id
  WHERE r.status='PENDING' AND r.financial_entry_id IS NULL AND c.total_amount>0
), inserted AS (
  INSERT INTO financial_entries(id,agency_id,customer_id,description,type,category,amount,entry_date,status,sale_amount,supplier_cost,commission_amount)
  SELECT entry_id,agency_id,customer_id,left('Recebimento previsto — '||title,1024),'INCOME','PACKAGE_SOLD',amount,due_date,'PENDING',amount,round(cost*amount/total_amount,2),0 FROM planned
  RETURNING id
)
UPDATE receivables r SET financial_entry_id=p.entry_id FROM planned p JOIN inserted i ON i.id=p.entry_id WHERE r.id=p.receivable_id;

-- Only create a pending supplier cost for unpaid sales. Historical cash is untouched.
INSERT INTO payables(id,sale_id,payable_type,amount,due_date,status)
SELECT gen_random_uuid(),c.sale_id,'SUPPLIER',c.cost,CURRENT_DATE,'PENDING'
FROM quotation_cost_repair c WHERE c.cost>0
  AND NOT EXISTS(SELECT 1 FROM payables p WHERE p.sale_id=c.sale_id AND p.payable_type='SUPPLIER')
  AND NOT EXISTS(SELECT 1 FROM receivables r WHERE r.sale_id=c.sale_id AND r.status='PAID');

WITH planned AS MATERIALIZED (
  SELECT p.id payable_id,gen_random_uuid() entry_id,p.amount,COALESCE(p.due_date,CURRENT_DATE) due_date,c.*
  FROM payables p JOIN quotation_cost_repair c ON c.sale_id=p.sale_id
  WHERE p.payable_type='SUPPLIER' AND p.status='PENDING' AND p.financial_entry_id IS NULL
), inserted AS (
  INSERT INTO financial_entries(id,agency_id,customer_id,description,type,category,amount,entry_date,status)
  SELECT entry_id,agency_id,customer_id,left('Custo previsto — '||title,1024),'EXPENSE','OTHER',amount,due_date,'PENDING' FROM planned RETURNING id
)
UPDATE payables p SET financial_entry_id=r.entry_id FROM planned r JOIN inserted i ON i.id=r.entry_id WHERE p.id=r.payable_id;

UPDATE trips t SET travel_start_date=COALESCE(c.start_date,t.travel_start_date),
 travel_end_date=COALESCE(c.end_date,t.travel_end_date), service_type='FLIGHT'
FROM quotation_cost_repair c WHERE t.quotation_id=c.quotation_id AND COALESCE(t.booking_locator,'')='';

INSERT INTO trips(id,agency_id,customer_id,quotation_id,sale_id,service_type,status,sale_date,travel_start_date,travel_end_date,notes)
SELECT gen_random_uuid(),c.agency_id,c.customer_id,c.quotation_id,c.sale_id,'FLIGHT','UPCOMING',s.sale_date,c.start_date,c.end_date,c.title
FROM quotation_cost_repair c JOIN sales s ON s.id=c.sale_id
WHERE NOT EXISTS(SELECT 1 FROM trips t WHERE t.quotation_id=c.quotation_id);
