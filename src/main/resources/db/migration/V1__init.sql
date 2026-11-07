create table users (
    id bigserial primary key,
    email varchar(255) not null unique,
    password_hash varchar(100) not null,
    role varchar(20) not null,
    status varchar(20) not null default 'ACTIVE',
    created_at timestamptz not null default now()
);

create table links (
    id bigserial primary key,
    short_code varchar(32) not null unique,
    original_url varchar(2048) not null,
    owner_id bigint not null references users (id),
    status varchar(20) not null default 'ACTIVE',
    expires_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index on links (owner_id, created_at desc);

create table clicks (
    id bigserial primary key,
    link_id bigint not null references links (id) on delete cascade,
    clicked_at timestamptz not null default now(),
    referer text,
    user_agent text,
    ip varchar(45)
);

create index on clicks (link_id, clicked_at);
