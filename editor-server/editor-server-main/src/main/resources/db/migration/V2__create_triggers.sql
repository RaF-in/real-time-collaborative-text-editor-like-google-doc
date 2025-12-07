-- Create a function to handle crdt_operation outbox events
CREATE OR REPLACE FUNCTION public.handle_crdt_operation_outbox_event()
RETURNS TRIGGER AS $$
DECLARE
event_type TEXT;
event_payload JSONB;
BEGIN
    -- Determine event type based on operation
CASE TG_OP
        WHEN 'INSERT' THEN
            event_type := 'CREATED';
            event_payload := row_to_json(NEW)::jsonb;
WHEN 'UPDATE' THEN
            event_type := 'UPDATED';
            event_payload := row_to_json(NEW)::jsonb;
WHEN 'DELETE' THEN
            event_type := 'DELETED';
            event_payload := row_to_json(OLD)::jsonb;
END CASE;

-- Insert into outbox_events table atomically
INSERT INTO public.crdt_operation_outbox_events (
    id, aggregate_type, type, payload, created_at
) VALUES (
             gen_random_uuid(), 'crdt_operation', event_type,
             event_payload, CURRENT_TIMESTAMP
         );
RETURN NULL;
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS crdt_operation_outbox_trigger ON public.crdt_operations;
-- Create triggers on crdt_operation and category tables
CREATE TRIGGER crdt_operation_outbox_trigger
    AFTER INSERT OR UPDATE OR DELETE ON public.crdt_operations
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_crdt_operation_outbox_event();

