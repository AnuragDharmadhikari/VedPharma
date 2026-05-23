--
-- PostgreSQL database dump
--



-- Dumped from database version 17.9
-- Dumped by pg_dump version 17.9

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: ai_interactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ai_interactions (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    cached_response boolean NOT NULL,
    completion_tokens integer NOT NULL,
    cost_usd numeric(10,8) NOT NULL,
    duration_ms bigint NOT NULL,
    entity_id uuid NOT NULL,
    feature character varying(255) NOT NULL,
    model_used character varying(255) NOT NULL,
    performed_by uuid NOT NULL,
    prompt_tokens integer NOT NULL,
    total_tokens integer NOT NULL
);


--
-- Name: audit_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_log (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    action character varying(255) NOT NULL,
    entity_id uuid,
    entity_type character varying(255),
    error_message text,
    result character varying(255) NOT NULL,
    user_email character varying(255) NOT NULL,
    user_id uuid,
    CONSTRAINT audit_log_result_check CHECK (((result)::text = ANY ((ARRAY['SUCCESS'::character varying, 'FAILURE'::character varying])::text[])))
);


--
-- Name: batches; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.batches (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    batch_number character varying(255) NOT NULL,
    current_quantity integer NOT NULL,
    expiry_date date NOT NULL,
    initial_quantity integer NOT NULL,
    mfg_date date NOT NULL,
    product_id uuid NOT NULL
);


--
-- Name: call_targets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.call_targets (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    actual_visits integer NOT NULL,
    month integer NOT NULL,
    target_visits integer NOT NULL,
    year integer NOT NULL,
    assigned_by_id uuid NOT NULL,
    rep_id uuid NOT NULL
);


--
-- Name: chemists; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.chemists (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    address text,
    city character varying(255) NOT NULL,
    drug_license_number character varying(255) NOT NULL,
    firm_name character varying(255) NOT NULL,
    gstin character varying(255),
    is_active boolean NOT NULL,
    owner_name character varying(255) NOT NULL,
    phone character varying(255) NOT NULL,
    state character varying(255) NOT NULL,
    assigned_rep_id uuid NOT NULL
);


--
-- Name: credit_note_number_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.credit_note_number_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: credit_notes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.credit_notes (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    amount numeric(10,2) NOT NULL,
    credit_note_number character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    applied_to_invoice_id uuid,
    chemist_id uuid,
    return_id uuid NOT NULL,
    stockist_id uuid,
    CONSTRAINT credit_notes_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'APPLIED'::character varying, 'VOID'::character varying])::text[])))
);


--
-- Name: doctors; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.doctors (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    city character varying(255) NOT NULL,
    email character varying(255),
    full_name character varying(255) NOT NULL,
    hospital_name character varying(255),
    is_active boolean NOT NULL,
    phone character varying(255),
    specialty character varying(255) NOT NULL,
    state character varying(255) NOT NULL,
    tier character varying(255) NOT NULL,
    territory_id uuid,
    CONSTRAINT doctors_tier_check CHECK (((tier)::text = ANY ((ARRAY['A'::character varying, 'B'::character varying, 'C'::character varying])::text[])))
);

--
-- Name: invoice_line_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invoice_line_items (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    cgst_amt numeric(10,2) NOT NULL,
    discount_pct numeric(10,2) NOT NULL,
    hsn_code character varying(255) NOT NULL,
    igst_amt numeric(10,2) NOT NULL,
    line_total numeric(10,2) NOT NULL,
    quantity integer NOT NULL,
    sgst_amt numeric(10,2) NOT NULL,
    taxable_amount numeric(10,2) NOT NULL,
    unit_price numeric(10,2) NOT NULL,
    invoice_id uuid NOT NULL,
    product_id uuid NOT NULL,
    free_quantity integer
);


--
-- Name: invoice_number_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.invoice_number_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: invoices; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.invoices (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    grand_total numeric(10,2) NOT NULL,
    invoice_date date NOT NULL,
    invoice_number character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    subtotal numeric(10,2) NOT NULL,
    tax_type character varying(255) NOT NULL,
    total_cgst numeric(10,2) NOT NULL,
    total_discount numeric(10,2) NOT NULL,
    total_igst numeric(10,2) NOT NULL,
    total_sgst numeric(10,2) NOT NULL,
    order_id uuid NOT NULL,
    rep_id uuid NOT NULL,
    billed_to character varying(255) NOT NULL,
    chemist_id uuid NOT NULL,
    stockist_id uuid,
    CONSTRAINT invoices_billed_to_check CHECK (((billed_to)::text = ANY ((ARRAY['STOCKIST'::character varying, 'CHEMIST'::character varying])::text[]))),
    CONSTRAINT invoices_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'ISSUED'::character varying, 'PARTIALLY_PAID'::character varying, 'PAID'::character varying])::text[]))),
    CONSTRAINT invoices_tax_type_check CHECK (((tax_type)::text = ANY ((ARRAY['CGST_SGST'::character varying, 'IGST'::character varying])::text[])))
);


--
-- Name: order_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_items (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    discount_pct numeric(10,2) NOT NULL,
    line_total numeric(10,2) NOT NULL,
    quantity integer NOT NULL,
    unit_price numeric(10,2) NOT NULL,
    order_id uuid NOT NULL,
    product_id uuid NOT NULL,
    free_quantity integer,
    scheme_discount_pct numeric(5,2)
);


--
-- Name: orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orders (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    order_date date NOT NULL,
    status character varying(255) NOT NULL,
    total_amount numeric(10,2) NOT NULL,
    rep_id uuid NOT NULL,
    stockist_id uuid,
    fulfillment_type character varying(255) NOT NULL,
    chemist_id uuid NOT NULL,
    CONSTRAINT orders_fulfillment_type_check CHECK (((fulfillment_type)::text = ANY ((ARRAY['VIA_STOCKIST'::character varying, 'DIRECT'::character varying])::text[]))),
    CONSTRAINT orders_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'CONFIRMED'::character varying, 'DISPATCHED'::character varying])::text[])))
);


--
-- Name: payment_allocations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payment_allocations (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    allocated_amount numeric(10,2) NOT NULL,
    invoice_id uuid NOT NULL,
    payment_id uuid NOT NULL
);


--
-- Name: payment_number_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.payment_number_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    amount numeric(10,2) NOT NULL,
    notes text,
    payment_date date NOT NULL,
    payment_mode character varying(255) NOT NULL,
    payment_number character varying(255) NOT NULL,
    reference_number character varying(255),
    chemist_id uuid,
    stockist_id uuid,
    CONSTRAINT payments_payment_mode_check CHECK (((payment_mode)::text = ANY ((ARRAY['CASH'::character varying, 'CHEQUE'::character varying, 'NEFT'::character varying, 'RTGS'::character varying, 'UPI'::character varying])::text[])))
);


--
-- Name: products; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.products (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    category character varying(255) NOT NULL,
    dealer_price numeric(10,2) NOT NULL,
    gst_rate character varying(255) NOT NULL,
    hsn_code character varying(255) NOT NULL,
    is_active boolean NOT NULL,
    molecule character varying(255) NOT NULL,
    mrp numeric(10,2) NOT NULL,
    name character varying(255) NOT NULL,
    CONSTRAINT products_gst_rate_check CHECK (((gst_rate)::text = ANY ((ARRAY['GST_5'::character varying, 'GST_12'::character varying, 'GST_18'::character varying])::text[])))
);


--
-- Name: return_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.return_items (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    condition character varying(255) NOT NULL,
    line_total numeric(10,2) NOT NULL,
    quantity integer NOT NULL,
    unit_price numeric(10,2) NOT NULL,
    batch_id uuid NOT NULL,
    product_id uuid NOT NULL,
    return_id uuid NOT NULL,
    CONSTRAINT return_items_condition_check CHECK (((condition)::text = ANY ((ARRAY['SALEABLE'::character varying, 'DAMAGED'::character varying, 'EXPIRED'::character varying])::text[])))
);


--
-- Name: return_number_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.return_number_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: returns; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.returns (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    reason text NOT NULL,
    return_date date NOT NULL,
    return_number character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    chemist_id uuid,
    stockist_id uuid,
    CONSTRAINT returns_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PROCESSED'::character varying, 'REJECTED'::character varying])::text[])))
);


--
-- Name: scheme_applications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.scheme_applications (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    benefit_description character varying(255) NOT NULL,
    discount_applied numeric(5,2),
    free_quantity integer,
    scheme_type character varying(255),
    order_item_id uuid NOT NULL,
    scheme_id uuid NOT NULL,
    CONSTRAINT scheme_applications_scheme_type_check CHECK (((scheme_type)::text = ANY ((ARRAY['QUANTITY_FREE'::character varying, 'PERCENTAGE_DISCOUNT'::character varying])::text[])))
);


--
-- Name: schemes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.schemes (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    discount_pct numeric(5,2),
    free_quantity integer,
    is_active boolean NOT NULL,
    min_quantity integer NOT NULL,
    scheme_type character varying(255) NOT NULL,
    valid_from date NOT NULL,
    valid_to date NOT NULL,
    chemist_id uuid,
    product_id uuid NOT NULL,
    stockist_id uuid,
    CONSTRAINT schemes_scheme_type_check CHECK (((scheme_type)::text = ANY ((ARRAY['QUANTITY_FREE'::character varying, 'PERCENTAGE_DISCOUNT'::character varying])::text[])))
);


--
-- Name: stock_movements; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stock_movements (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    movement_type character varying(255) NOT NULL,
    notes text,
    quantity integer NOT NULL,
    reference_id uuid,
    reference_type character varying(255),
    batch_id uuid NOT NULL,
    CONSTRAINT stock_movements_movement_type_check CHECK (((movement_type)::text = ANY ((ARRAY['INWARD'::character varying, 'SALE'::character varying, 'RETURN'::character varying, 'SAMPLE'::character varying, 'ADJUSTMENT'::character varying, 'EXPIRY_WRITEOFF'::character varying])::text[])))
);


--
-- Name: stockists; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stockists (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    address text,
    city character varying(255) NOT NULL,
    firm_name character varying(255) NOT NULL,
    gstin character varying(255),
    is_active boolean NOT NULL,
    owner_name character varying(255) NOT NULL,
    phone character varying(255) NOT NULL,
    state character varying(255) NOT NULL,
    assigned_rep_id uuid NOT NULL
);


--
-- Name: territories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.territories (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    name character varying(255) NOT NULL,
    state character varying(255) NOT NULL,
    zone character varying(255),
    is_active boolean NOT NULL
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    email character varying(255) NOT NULL,
    full_name character varying(255) NOT NULL,
    is_active boolean NOT NULL,
    password_hash character varying(255) NOT NULL,
    phone character varying(255),
    role character varying(255) NOT NULL,
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['OWNER'::character varying, 'MANAGER'::character varying, 'REP'::character varying])::text[])))
);


--
-- Name: visit_products; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.visit_products (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    feedback text,
    sample_given integer,
    product_id uuid NOT NULL,
    visit_id uuid NOT NULL,
    samples_given integer,
    batch_id uuid
);


--
-- Name: visits; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.visits (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    ai_summary text,
    notes text,
    status character varying(255) NOT NULL,
    visit_date date NOT NULL,
    doctor_id uuid NOT NULL,
    rep_id uuid NOT NULL,
    CONSTRAINT visits_status_check CHECK (((status)::text = ANY ((ARRAY['PLANNED'::character varying, 'COMPLETED'::character varying, 'MISSED'::character varying])::text[])))
);


--
-- Name: ai_interactions ai_interactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ai_interactions
    ADD CONSTRAINT ai_interactions_pkey PRIMARY KEY (id);


--
-- Name: audit_log audit_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id);


--
-- Name: batches batches_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.batches
    ADD CONSTRAINT batches_pkey PRIMARY KEY (id);


--
-- Name: call_targets call_targets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.call_targets
    ADD CONSTRAINT call_targets_pkey PRIMARY KEY (id);


--
-- Name: chemists chemists_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chemists
    ADD CONSTRAINT chemists_pkey PRIMARY KEY (id);


--
-- Name: credit_notes credit_notes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_notes
    ADD CONSTRAINT credit_notes_pkey PRIMARY KEY (id);


--
-- Name: doctors doctors_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doctors
    ADD CONSTRAINT doctors_pkey PRIMARY KEY (id);

--
-- Name: invoice_line_items invoice_line_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_line_items
    ADD CONSTRAINT invoice_line_items_pkey PRIMARY KEY (id);


--
-- Name: invoices invoices_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT invoices_pkey PRIMARY KEY (id);


--
-- Name: order_items order_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id);


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);


--
-- Name: payment_allocations payment_allocations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_allocations
    ADD CONSTRAINT payment_allocations_pkey PRIMARY KEY (id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);


--
-- Name: return_items return_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.return_items
    ADD CONSTRAINT return_items_pkey PRIMARY KEY (id);


--
-- Name: returns returns_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.returns
    ADD CONSTRAINT returns_pkey PRIMARY KEY (id);


--
-- Name: scheme_applications scheme_applications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scheme_applications
    ADD CONSTRAINT scheme_applications_pkey PRIMARY KEY (id);


--
-- Name: schemes schemes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schemes
    ADD CONSTRAINT schemes_pkey PRIMARY KEY (id);


--
-- Name: stock_movements stock_movements_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_movements
    ADD CONSTRAINT stock_movements_pkey PRIMARY KEY (id);


--
-- Name: stockists stockists_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stockists
    ADD CONSTRAINT stockists_pkey PRIMARY KEY (id);


--
-- Name: territories territories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.territories
    ADD CONSTRAINT territories_pkey PRIMARY KEY (id);


--
-- Name: batches uk_batches_product_batch_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.batches
    ADD CONSTRAINT uk_batches_product_batch_number UNIQUE (product_id, batch_number);


--
-- Name: call_targets uk_call_targets_rep_month_year; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.call_targets
    ADD CONSTRAINT uk_call_targets_rep_month_year UNIQUE (rep_id, month, year);


--
-- Name: users uk_users_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_users_email UNIQUE (email);


--
-- Name: visits uk_visits_doctor_date; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.visits
    ADD CONSTRAINT uk_visits_doctor_date UNIQUE (doctor_id, visit_date);


--
-- Name: credit_notes ukabchkricq0sc3vdu6x1eseabr; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_notes
    ADD CONSTRAINT ukabchkricq0sc3vdu6x1eseabr UNIQUE (credit_note_number);


--
-- Name: payments ukc6nxg52ow66u8ut91bytspy64; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT ukc6nxg52ow66u8ut91bytspy64 UNIQUE (payment_number);


--
-- Name: invoices uke718q5klx5pempy28p2nx88a6; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT uke718q5klx5pempy28p2nx88a6 UNIQUE (order_id);


--
-- Name: chemists uki0kya3qx924h5di4joounrlf9; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chemists
    ADD CONSTRAINT uki0kya3qx924h5di4joounrlf9 UNIQUE (drug_license_number);


--
-- Name: stockists ukisqjt8wmgi5w4905siqsem4s0; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stockists
    ADD CONSTRAINT ukisqjt8wmgi5w4905siqsem4s0 UNIQUE (gstin);


--
-- Name: credit_notes ukj2ld7q2dhunnio4r2mvr8xrhl; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_notes
    ADD CONSTRAINT ukj2ld7q2dhunnio4r2mvr8xrhl UNIQUE (return_id);


--
-- Name: chemists ukkac1rn7k6he7g3h4dpn6utl2u; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chemists
    ADD CONSTRAINT ukkac1rn7k6he7g3h4dpn6utl2u UNIQUE (gstin);


--
-- Name: invoices ukl1x55mfsay7co0r3m9ynvipd5; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT ukl1x55mfsay7co0r3m9ynvipd5 UNIQUE (invoice_number);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: visit_products visit_products_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.visit_products
    ADD CONSTRAINT visit_products_pkey PRIMARY KEY (id);


--
-- Name: visits visits_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.visits
    ADD CONSTRAINT visits_pkey PRIMARY KEY (id);


--
-- Name: payment_allocations fk12k58td8oudl7ihiuvyuprf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_allocations
    ADD CONSTRAINT fk12k58td8oudl7ihiuvyuprf FOREIGN KEY (invoice_id) REFERENCES public.invoices(id);


--
-- Name: call_targets fk19xu0ojvhmcaxd29mqxfa2swm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.call_targets
    ADD CONSTRAINT fk19xu0ojvhmcaxd29mqxfa2swm FOREIGN KEY (assigned_by_id) REFERENCES public.users(id);


--
-- Name: schemes fk1miwb00rucxwtaa5umsxi2mwq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schemes
    ADD CONSTRAINT fk1miwb00rucxwtaa5umsxi2mwq FOREIGN KEY (stockist_id) REFERENCES public.stockists(id);


--
-- Name: invoices fk2dc4n221gdt1ejo8soeb4popr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT fk2dc4n221gdt1ejo8soeb4popr FOREIGN KEY (rep_id) REFERENCES public.users(id);


--
-- Name: credit_notes fk3bct7ma4jgvsb5ceg4og1msr4; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_notes
    ADD CONSTRAINT fk3bct7ma4jgvsb5ceg4og1msr4 FOREIGN KEY (return_id) REFERENCES public.returns(id);


--
-- Name: invoices fk4ko3y00tkkk2ya3p6wnefjj2f; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT fk4ko3y00tkkk2ya3p6wnefjj2f FOREIGN KEY (order_id) REFERENCES public.orders(id);


--
-- Name: return_items fk5a4moov85fw9ixxf9l0mki44o; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.return_items
    ADD CONSTRAINT fk5a4moov85fw9ixxf9l0mki44o FOREIGN KEY (return_id) REFERENCES public.returns(id);


--
-- Name: visit_products fk5qc572qc61147ujtmcngvq4np; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.visit_products
    ADD CONSTRAINT fk5qc572qc61147ujtmcngvq4np FOREIGN KEY (batch_id) REFERENCES public.batches(id);


--
-- Name: invoice_line_items fk62yq0tvg2ma4y8plfgx34bd3l; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_line_items
    ADD CONSTRAINT fk62yq0tvg2ma4y8plfgx34bd3l FOREIGN KEY (invoice_id) REFERENCES public.invoices(id);


--
-- Name: orders fk6cmu2atce3xowmdsfl6e90ah9; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fk6cmu2atce3xowmdsfl6e90ah9 FOREIGN KEY (rep_id) REFERENCES public.users(id);


--
-- Name: schemes fk77aulix2yux2nfa5u6qojhvxb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schemes
    ADD CONSTRAINT fk77aulix2yux2nfa5u6qojhvxb FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: credit_notes fk9te0iwpo02393j7lu6p8vfaro; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_notes
    ADD CONSTRAINT fk9te0iwpo02393j7lu6p8vfaro FOREIGN KEY (applied_to_invoice_id) REFERENCES public.invoices(id);


--
-- Name: order_items fkbioxgbv59vetrxe0ejfubep1w; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT fkbioxgbv59vetrxe0ejfubep1w FOREIGN KEY (order_id) REFERENCES public.orders(id);


--
-- Name: scheme_applications fkboa2a5i9rkhel4tixfocmwqw7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scheme_applications
    ADD CONSTRAINT fkboa2a5i9rkhel4tixfocmwqw7 FOREIGN KEY (scheme_id) REFERENCES public.schemes(id);


--
-- Name: returns fkd01bhbl95nsga7xq7lws0j6rf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.returns
    ADD CONSTRAINT fkd01bhbl95nsga7xq7lws0j6rf FOREIGN KEY (stockist_id) REFERENCES public.stockists(id);


--
-- Name: payments fke4exnykj0tjqh0klbnhr2n28q; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fke4exnykj0tjqh0klbnhr2n28q FOREIGN KEY (chemist_id) REFERENCES public.chemists(id);


--
-- Name: call_targets fkepxfxva9t6e8hdemy9ducrwse; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.call_targets
    ADD CONSTRAINT fkepxfxva9t6e8hdemy9ducrwse FOREIGN KEY (rep_id) REFERENCES public.users(id);


--
-- Name: chemists fkew01vy3ofi6atb4japii7p7v2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.chemists
    ADD CONSTRAINT fkew01vy3ofi6atb4japii7p7v2 FOREIGN KEY (assigned_rep_id) REFERENCES public.users(id);


--
-- Name: payment_allocations fkf6kmlajje9ey0ae5kr2u71xdu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_allocations
    ADD CONSTRAINT fkf6kmlajje9ey0ae5kr2u71xdu FOREIGN KEY (payment_id) REFERENCES public.payments(id);


--
-- Name: return_items fkfjgyw69bfdrt1jtnqxwwrudyc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.return_items
    ADD CONSTRAINT fkfjgyw69bfdrt1jtnqxwwrudyc FOREIGN KEY (batch_id) REFERENCES public.batches(id);


--
-- Name: visit_products fkhtpbn2pawapsjfktpc3r2ekvu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.visit_products
    ADD CONSTRAINT fkhtpbn2pawapsjfktpc3r2ekvu FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: orders fkjaok76tijw99c9ddxcpjy671u; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fkjaok76tijw99c9ddxcpjy671u FOREIGN KEY (chemist_id) REFERENCES public.chemists(id);


--
-- Name: batches fkjb38v1mk479a6t6ay2mewo03m; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.batches
    ADD CONSTRAINT fkjb38v1mk479a6t6ay2mewo03m FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: orders fkjevo1k0sp0rwri5wfgph1hyhk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fkjevo1k0sp0rwri5wfgph1hyhk FOREIGN KEY (stockist_id) REFERENCES public.stockists(id);


--
-- Name: stock_movements fkknflxm8nu08vs8bpwqdsv1rdo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_movements
    ADD CONSTRAINT fkknflxm8nu08vs8bpwqdsv1rdo FOREIGN KEY (batch_id) REFERENCES public.batches(id);


--
-- Name: invoices fkl541jxvhm0smxunbxvkt00665; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT fkl541jxvhm0smxunbxvkt00665 FOREIGN KEY (chemist_id) REFERENCES public.chemists(id);


--
-- Name: schemes fklednyuxwo7i859lyo5fs2mwyc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schemes
    ADD CONSTRAINT fklednyuxwo7i859lyo5fs2mwyc FOREIGN KEY (chemist_id) REFERENCES public.chemists(id);


--
-- Name: stockists fkmd9egb0qo0ekpp6vyrua24v9j; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stockists
    ADD CONSTRAINT fkmd9egb0qo0ekpp6vyrua24v9j FOREIGN KEY (assigned_rep_id) REFERENCES public.users(id);


--
-- Name: visits fkmxgmsngmj1f2jfnbf4r9vn3n0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.visits
    ADD CONSTRAINT fkmxgmsngmj1f2jfnbf4r9vn3n0 FOREIGN KEY (rep_id) REFERENCES public.users(id);


--
-- Name: order_items fkocimc7dtr037rh4ls4l95nlfi; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT fkocimc7dtr037rh4ls4l95nlfi FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: returns fkp6dn7355evoi2tvfc4w2e891r; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.returns
    ADD CONSTRAINT fkp6dn7355evoi2tvfc4w2e891r FOREIGN KEY (chemist_id) REFERENCES public.chemists(id);


--
-- Name: payments fkp9nhyv10o51grx0n03opn7xy6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fkp9nhyv10o51grx0n03opn7xy6 FOREIGN KEY (stockist_id) REFERENCES public.stockists(id);


--
-- Name: scheme_applications fkpm71o4ljr8voi0a1qp647oexg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scheme_applications
    ADD CONSTRAINT fkpm71o4ljr8voi0a1qp647oexg FOREIGN KEY (order_item_id) REFERENCES public.order_items(id);


--
-- Name: return_items fkqhmywyq6yw2l17f20ejedaaq9; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.return_items
    ADD CONSTRAINT fkqhmywyq6yw2l17f20ejedaaq9 FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: visit_products fkqtx4g7ue253cj8sffjpym4ixj; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.visit_products
    ADD CONSTRAINT fkqtx4g7ue253cj8sffjpym4ixj FOREIGN KEY (visit_id) REFERENCES public.visits(id);


--
-- Name: credit_notes fkrwie0p8jorsk6r24ncphwfd8h; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_notes
    ADD CONSTRAINT fkrwie0p8jorsk6r24ncphwfd8h FOREIGN KEY (chemist_id) REFERENCES public.chemists(id);


--
-- Name: invoices fks0txq4vdv08vxpy49knoqb8pd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoices
    ADD CONSTRAINT fks0txq4vdv08vxpy49knoqb8pd FOREIGN KEY (stockist_id) REFERENCES public.stockists(id);


--
-- Name: invoice_line_items fksqs6npb85l1r7kuhgds959t4c; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.invoice_line_items
    ADD CONSTRAINT fksqs6npb85l1r7kuhgds959t4c FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: doctors fktg2jxilwrnl3qj9yuq4daet6y; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.doctors
    ADD CONSTRAINT fktg2jxilwrnl3qj9yuq4daet6y FOREIGN KEY (territory_id) REFERENCES public.territories(id);


--
-- Name: visits fkth95fndjk3y3nepjfu3f66r63; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.visits
    ADD CONSTRAINT fkth95fndjk3y3nepjfu3f66r63 FOREIGN KEY (doctor_id) REFERENCES public.doctors(id);


--
-- Name: credit_notes fkuawt0vacbpkm5x4woc8i7919; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.credit_notes
    ADD CONSTRAINT fkuawt0vacbpkm5x4woc8i7919 FOREIGN KEY (stockist_id) REFERENCES public.stockists(id);


--
-- PostgreSQL database dump complete
--



