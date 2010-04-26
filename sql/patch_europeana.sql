-- patch-fil f�r att uppdatera ett schema f�r att komma ih�g borttagna poster f�r europeana

alter table content add (added timestamp);
alter table content add (deleted timestamp);
alter table content add (datestamp timestamp);
alter table content add (status integer);

create index ix_content_serv_status on content (serviceId, status) tablespace KSAMSOK_INDX;
create index ix_content_serv_deleted on content (serviceId, deleted) tablespace KSAMSOK_INDX;
create index ix_content_uri_serv on content (uri, serviceId) tablespace KSAMSOK_INDX;
create index ix_content_date on content (datestamp) tablespace KSAMSOK_INDX;
create index ix_content_deleted on content (deleted, ' ') tablespace KSAMSOK_INDX;

-- uppdatera status (tar nog en stund)
update content set status = 0;
commit;

-- och g�r den till not null
alter table content modify status not null;

-- ful-sql f�r att parsa ut datum fr�n xml (vilket verkar funka i utvdb i alla fall) och s�tta datestamp
-- tar nog mellan 6 och 8h att k�ra...
update content set datestamp = (
case when INSTR(xmldata, 'lastChangedDate>') > 0 then
  case when to_char(substr(xmldata, INSTR(xmldata, 'lastChangedDate>') + 20, 1)) = '-' then
      to_date(substr(xmldata, INSTR(xmldata, 'lastChangedDate>') + 16, 10), 'YYYY-MM-DD')
  else
     changed
  end
else
  changed
end);

commit;

-- och g�r datestamp till not null
alter table content modify datestamp not null;
