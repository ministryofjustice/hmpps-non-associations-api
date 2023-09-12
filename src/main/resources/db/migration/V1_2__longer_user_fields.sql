alter table non_association
    alter column authorised_by type varchar(60);

alter table non_association
    alter column updated_by type varchar(60);

alter table non_association
    alter column closed_by type varchar(60);
