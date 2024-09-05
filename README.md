```sql
\copy (select ar.id,ar.deploypath,ar.identifier,ar.purl from artifact ar join targetrepository tr on ar.targetrepository_id = tr.id and tr.repositorytype = 'MAVEN') to '/tmp/test.csv' delimiter ',' csv;
```
