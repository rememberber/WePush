create table if not exists t_msg_mp_subscribe
(
    id            integer
        constraint t_msg_mp_subscribe_pk
            primary key autoincrement,
    msg_type      integer,
    msg_name      text,
    template_id   text,
    url           text,
    ma_appid      text,
    ma_page_path  text,
    preview_user  text,
    wx_account_id  int,
    create_time   datetime,
    modified_time datetime
);
create unique index if not exists t_msg_mp_subscribe_msg_type_msg_name_uindex
    on t_msg_mp_subscribe (msg_type, msg_name);