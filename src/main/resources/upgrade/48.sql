alter table t_msg_ding
    add account_id integer;
alter table t_msg_http
    add account_id integer;
alter table t_msg_kefu
    add account_id integer;
alter table t_msg_kefu_priority
    add account_id integer;
alter table t_msg_ma_subscribe
    add account_id integer;
alter table t_msg_ma_template
    add account_id integer;
alter table t_msg_mail
    add account_id integer;
alter table t_msg_mp_subscribe
    add account_id integer;
alter table t_msg_mp_template
    add account_id integer;
alter table t_msg_sms
    add account_id integer;
alter table t_msg_wx_cp
    add account_id integer;
alter table t_msg_wx_uniform
    add account_id integer;

create unique index if not exists t_msg_ding_msg_type_account_id_msg_name_uindex
    on t_msg_ding (msg_type, account_id, msg_name);
create unique index if not exists t_msg_http_msg_type_account_id_msg_name_uindex
    on t_msg_http (msg_type, account_id, msg_name);
create unique index if not exists t_msg_kefu_msg_type_account_id_msg_name_uindex
    on t_msg_kefu (msg_type, account_id, msg_name);
create unique index if not exists t_msg_kefu_priority_msg_type_account_id_msg_name_uindex
    on t_msg_kefu_priority (msg_type, account_id, msg_name);
create unique index if not exists t_msg_ma_subscribe_msg_type_account_id_msg_name_uindex
    on t_msg_ma_subscribe (msg_type, account_id, msg_name);
create unique index if not exists t_msg_ma_template_msg_type_account_id_msg_name_uindex
    on t_msg_ma_template (msg_type, account_id, msg_name);
create unique index if not exists t_msg_mail_msg_type_account_id_msg_name_uindex
    on t_msg_mail (msg_type, account_id, msg_name);
create unique index if not exists t_msg_mp_subscribe_msg_type_account_id_msg_name_uindex
    on t_msg_mp_subscribe (msg_type, account_id, msg_name);
create unique index if not exists t_msg_mp_template_msg_type_account_id_msg_name_uindex
    on t_msg_mp_template (msg_type, account_id, msg_name);
create unique index if not exists t_msg_sms_msg_type_account_id_msg_name_uindex
    on t_msg_sms (msg_type, account_id, msg_name);
create unique index if not exists t_msg_wx_cp_msg_type_account_id_msg_name_uindex
    on t_msg_wx_cp (msg_type, account_id, msg_name);
create unique index if not exists t_msg_wx_uniform_msg_type_account_id_msg_name_uindex
    on t_msg_wx_uniform (msg_type, account_id, msg_name);