create table if not exists t_msg_kefu
(
    id            integer
        constraint t_msg_kefu_pk
            primary key autoincrement,
    msg_type      integer,
    msg_name      text,
    kefu_msg_type text,
    content       text,
    title         text,
    img_url       text,
    describe      text,
    url           text,
    create_time   datetime,
    modified_time datetime
);
create unique index if not exists t_msg_kefu_msg_type_msg_name_uindex
    on t_msg_kefu (msg_type, msg_name);
create table if not exists t_msg_kefu_priority
(
    id            integer
        constraint t_msg_kefu_priority_pk
            primary key autoincrement,
    msg_type      integer,
    msg_name      text,
    template_id   text,
    url           text,
    ma_appid      text,
    ma_page_path  text,
    kefu_msg_type text,
    content       text,
    title         text,
    img_url       text,
    describe      text,
    kefu_url      text,
    create_time   datetime,
    modified_time datetime
);
create unique index if not exists t_msg_kefu_priority_msg_type_msg_name_uindex
    on t_msg_kefu_priority (msg_type, msg_name);
create table if not exists t_msg_ma_template
(
    id               integer
        constraint t_msg_ma_template_pk
            primary key autoincrement,
    msg_type         integer,
    msg_name         text,
    template_id      text,
    page             text,
    emphasis_keyword text,
    create_time      datetime,
    modified_time    datetime
);
create unique index if not exists t_msg_ma_template_msg_type_msg_name_uindex
    on t_msg_ma_template (msg_type, msg_name);
create table if not exists t_msg_mp_template
(
    id            integer
        constraint t_msg_mp_template_pk
            primary key autoincrement,
    msg_type      integer,
    msg_name      text,
    template_id   text,
    url           text,
    ma_appid      text,
    ma_page_path  text,
    create_time   datetime,
    modified_time datetime
);
create unique index if not exists t_msg_mp_template_msg_type_msg_name_uindex
    on t_msg_mp_template (msg_type, msg_name);
create table if not exists t_msg_sms
(
    id            integer
        constraint t_msg_ali_template_pk
            primary key autoincrement,
    msg_type      integer,
    msg_name      text,
    template_id   text,
    content       text,
    create_time   datetime,
    modified_time datetime
);
create unique index if not exists t_msg_sms_msg_type_msg_name_uindex
    on t_msg_sms (msg_type, msg_name);
create table if not exists t_push_history
(
    id            integer
        constraint t_push_history_pk
            primary key autoincrement,
    msg_id        integer,
    msg_type      integer,
    msg_name      text,
    result        text,
    csv_file      text,
    create_time   datetime,
    modified_time datetime
);
create table if not exists t_template_data
(
    id            integer
        constraint t_template_data_ma_pk
            primary key autoincrement,
    msg_type      integer,
    msg_id        integer,
    name          text,
    value         text,
    color         text,
    create_time   datetime,
    modified_time datetime
);
create table if not exists t_wx_mp_user
(
    open_id         text
        constraint t_wx_mp_user_pk
            primary key,
    nickname        text,
    sex_desc        text,
    sex             INTEGER,
    language        text,
    city            text,
    province        text,
    country         text,
    head_img_url    text,
    subscribe_time  text,
    union_id        text,
    remark          text,
    group_id        integer,
    subscribe_scene text,
    qr_scene        text,
    qr_scene_str    text,
    create_time     text,
    modified_time   text
);
create table if not exists t_wx_account
(
    id            INTEGER
        constraint t_wx_account_pk
            primary key autoincrement,
    account_type  text,
    account_name  text,
    app_id        text,
    app_secret    text,
    token         text,
    aes_key       text,
    create_time   datetime,
    modified_time datetime
);

create table if not exists t_msg_mail
(
    id            integer
        constraint t_msg_mail_pk
            primary key autoincrement,
    msg_type      integer,
    msg_name      text,
    title         text,
    Cc            text,
    files         text,
    content       text,
    create_time   datetime,
    modified_time datetime
);

create unique index if not exists t_msg_mail_msg_type_msg_name_uindex
    on t_msg_mail (msg_type, msg_name);

create table if not exists t_wx_cp_app
(
    id            INTEGER
        constraint t_wx_cp_app_pk
            primary key autoincrement,
    corpId        text,
    app_name      text,
    agent_id      text,
    secret        text,
    token         text,
    aes_key       text,
    create_time   datetime,
    modified_time datetime
);

create table if not exists t_msg_wx_cp
(
    id            integer
        constraint t_msg_wx_cp_pk
            primary key autoincrement,
    msg_type      integer,
    msg_name      text,
    cp_msg_type   text,
    agent_id      text,
    content       text,
    title         text,
    img_url       text,
    describe      text,
    url           text,
    btn_txt       text,
    create_time   datetime,
    modified_time datetime
);

create unique index if not exists t_msg_wx_cp_msg_type_msg_name_uindex
    on t_msg_wx_cp (msg_type, msg_name);

create table if not exists t_msg_http
(
    id            integer
        constraint t_msg_http_pk
            primary key autoincrement,
    msg_type      integer,
    msg_name      text,
    method        text,
    url           text,
    params        text,
    headers       text,
    cookies       text,
    body          text,
    body_type     text,
    create_time   datetime,
    modified_time datetime
);

create unique index if not exists t_msg_http_msg_type_msg_name_uindex
    on t_msg_http (msg_type, msg_name);

create table if not exists t_msg_ding
(
    id            integer
        constraint t_msg_ding_pk
            primary key autoincrement,
    msg_type      integer,
    msg_name      text,
    radio_type    text,
    ding_msg_type text,
    agent_id      text,
    web_hook      text,
    content       text,
    create_time   datetime,
    modified_time datetime
);

create unique index if not exists t_msg_ding_msg_type_msg_name_uindex
    on t_msg_ding (msg_type, msg_name);

create table if not exists t_ding_app
(
    id            INTEGER
        constraint t_ding_app_pk
            primary key autoincrement,
    app_name      text,
    agent_id      text,
    app_key       text,
    app_secret    text,
    create_time   datetime,
    modified_time datetime
);

create table if not exists t_msg_wx_uniform
(
    id               integer
        constraint t_msg_wx_uniform_pk
            primary key autoincrement,
    msg_type         integer,
    msg_name         text,
    mp_template_id   text,
    ma_template_id   text,
    mp_url           text,
    ma_appid         text,
    ma_page_path     text,
    page             text,
    emphasis_keyword text,
    create_time      datetime,
    modified_time    datetime
);

create unique index if not exists t_msg_wx_uniform_msg_type_msg_name_uindex
    on t_msg_wx_uniform (msg_type, msg_name);

create table if not exists t_msg_ma_subscribe
(
    id            integer
        constraint t_msg_ma_subscribe_pk
            primary key autoincrement,
    msg_type      integer,
    msg_name      text,
    template_id   text,
    page          text,
    create_time   datetime,
    modified_time datetime
);

create unique index if not exists t_msg_ma_subscribe_msg_type_msg_name_uindex
    on t_msg_ma_subscribe (msg_type, msg_name);

