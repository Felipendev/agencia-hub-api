-- TODO-053/Codex. Reserva central V65, base publicada V64. Histórico de geração, sem backfill.
CREATE TABLE simulation_task_generations (
 id UUID PRIMARY KEY, agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
 source_id UUID NOT NULL, simulation_id UUID REFERENCES accumulation_simulations(id) ON DELETE SET NULL,
 customer_id UUID NOT NULL REFERENCES customers(id), source_version INT NOT NULL CHECK(source_version>0),
 source_title VARCHAR(200) NOT NULL, snapshot JSONB NOT NULL, request_payload JSONB NOT NULL,
 author_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(agency_id,source_id)
);
CREATE INDEX simulation_task_generations_customer_idx ON simulation_task_generations(agency_id,customer_id);
CREATE TABLE simulation_task_links (
 agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
 generation_id UUID NOT NULL REFERENCES simulation_task_generations(id) ON DELETE CASCADE,
 task_id UUID PRIMARY KEY REFERENCES work_tasks(id) ON DELETE CASCADE,
 position INT NOT NULL CHECK(position BETWEEN 0 AND 19), UNIQUE(generation_id,position)
);
CREATE INDEX simulation_task_links_generation_idx ON simulation_task_links(agency_id,generation_id);
CREATE FUNCTION validate_simulation_task_link() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM simulation_task_generations g JOIN work_tasks t ON t.id=NEW.task_id
   WHERE g.id=NEW.generation_id AND g.agency_id=NEW.agency_id AND t.agency_id=g.agency_id
   AND t.customer_id=g.customer_id AND t.product='MILES' AND t.run_id IS NULL) THEN
   RAISE EXCEPTION 'Tarefa e origem devem pertencer ao mesmo cliente e agencia no produto Milhas' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER simulation_task_link_scope BEFORE INSERT OR UPDATE ON simulation_task_links
 FOR EACH ROW EXECUTE FUNCTION validate_simulation_task_link();
CREATE FUNCTION preserve_simulation_task_person() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM simulation_task_links l JOIN simulation_task_generations g ON g.id=l.generation_id
   WHERE l.task_id=NEW.id AND (NEW.agency_id<>g.agency_id OR NEW.customer_id<>g.customer_id OR NEW.product<>'MILES' OR NEW.run_id IS NOT NULL)) THEN
   RAISE EXCEPTION 'Tarefa gerada preserva pessoa e produto de origem' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER simulation_task_preserve_person BEFORE UPDATE OF agency_id,customer_id,product,run_id ON work_tasks
 FOR EACH ROW EXECUTE FUNCTION preserve_simulation_task_person();
