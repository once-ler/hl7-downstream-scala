if exists(select 1 from sys.objects where object_id = object_id('ril.wsi_execution_hist') and type in (N'U') )
drop table ril.wsi_execution_hist;
 
create table ril.wsi_execution_hist(
  id uniqueidentifier not null default newid(),
  start_time datetime default current_timestamp,
  from_store varchar(15),
  to_store varchar(15),
  study_id varchar(120),
  wsi varchar(50),
  caller varchar(100),
  request varchar(max),
  response varchar(max),
  error bit
);
 
IF  EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID(N'ril.wsi_execution_hist') AND name = N'idx_unique_wsi_execution_hist')
DROP INDEX idx_unique_wsi_execution_hist ON ril.wsi_execution_hist WITH ( ONLINE = off )
GO
 
create unique nonclustered index idx_unique_wsi_execution_hist on ril.wsi_execution_hist(
  id
) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = off, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
go
 
IF  EXISTS (SELECT * FROM sys.indexes WHERE object_id = OBJECT_ID(N'ril.wsi_execution_hist') AND name = N'idx_clustered_wsi_execution_hist')
DROP INDEX idx_clustered_wsi_execution_hist ON ril.wsi_execution_hist WITH ( ONLINE = OFF )
GO
 
create unique clustered INDEX idx_clustered_wsi_execution_hist ON ril.wsi_execution_hist
(
  start_time asc,
  from_store asc,
  to_store asc,
  study_id asc,
  wsi asc
) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
 
-- non clustered index
if  exists (select * from sys.indexes WHERE object_id = OBJECT_ID(N'ril.wsi_execution_hist') AND name = N'idx_wsi_execution_hist_0')
drop index [idx_wsi_execution_hist_0] ON ril.wsi_execution_hist WITH ( ONLINE = OFF )
GO
 
create nonclustered index [idx_wsi_execution_hist_0] ON ril.wsi_execution_hist
(
  caller asc
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
go
 
create nonclustered index [idx_wsi_execution_hist_1] ON ril.wsi_execution_hist
(
  wsi asc
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
go
 
create nonclustered index [idx_wsi_execution_hist_2] ON ril.wsi_execution_hist
(
  from_store asc
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
go
 
create nonclustered index [idx_wsi_execution_hist_3] ON ril.wsi_execution_hist
(
  to_store asc
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
go
 
create nonclustered index [idx_wsi_execution_hist_4] ON ril.wsi_execution_hist
(
  error asc
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
go
 
create nonclustered index [idx_wsi_execution_hist_5] ON ril.wsi_execution_hist
(
  study_id asc
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
go

insert ril.wsi_execution_hist (start_time, from_store, to_store, study_id, caller, request, response, error)
select current_timestamp, 'store_abc', 'store_def', '19-99998', 'some_proc', '<process>this</process>','Some error and stack trace\nblah blah\n...', 1;
go
insert ril.wsi_execution_hist (start_time, from_store, to_store, study_id, caller, request, response, error)
select current_timestamp, 'store_abc', 'store_def', '19-99997', 'some_proc', '<process>this</process>','Some error and stack trace\nblah blah\n...', 1;
go
insert ril.wsi_execution_hist (start_time, from_store, to_store, study_id, caller, request, response, error)
select current_timestamp, 'store_abc', 'store_def', '19-99996', 'some_proc', '<process>this</process>','Some error and stack trace\nblah blah\n...', 1;
go
insert ril.wsi_execution_hist (start_time, from_store, to_store, study_id, caller, request, response, error)
select current_timestamp, 'store_abc', 'store_def', '19-99995', 'some_proc', '<process>this</process>','Some error and stack trace\nblah blah\n...', 1;
go
insert ril.wsi_execution_hist (start_time, from_store, to_store, study_id, caller, request, response, error)
select current_timestamp, 'store_abc', 'store_def', '19-99994', 'some_proc', '<process>this</process>','Some error and stack trace\nblah blah\n...', 1;
go
insert ril.wsi_execution_hist (start_time, from_store, to_store, study_id, caller, request, response, error)
select current_timestamp, 'store_abc', 'store_def', '19-99993', 'some_proc', '<process>this</process>','Some error and stack trace\nblah blah\n...', 1;
go
insert ril.wsi_execution_hist (start_time, from_store, to_store, study_id, caller, request, response, error)
select current_timestamp, 'store_abc', 'store_def', '19-99992', 'some_proc', '<process>this</process>','Some error and stack trace\nblah blah\n...', 1;
go
insert ril.wsi_execution_hist (start_time, from_store, to_store, study_id, caller, request, response, error)
select current_timestamp, 'store_abc', 'store_def', '19-99991', 'some_proc', '<process>this</process>','Some error and stack trace\nblah blah\n...', 1;
go
insert ril.wsi_execution_hist (start_time, from_store, to_store, study_id, caller, request, response, error)
select current_timestamp, 'store_abc', 'store_def', '19-99990', 'some_proc', '<process>this</process>','Some error and stack trace\nblah blah\n...', 1;
go
insert ril.wsi_execution_hist (start_time, from_store, to_store, study_id, caller, request, response, error)
select current_timestamp, 'store_abc', 'store_def', '19-99989', 'some_proc', '<process>this</process>','Some error and stack trace\nblah blah\n...', 1;
go
