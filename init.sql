create table books
(
    id        bigint generated by default as identity
        primary key,
    author    varchar(255) not null,
    book_uid  uuid         not null,
    condition varchar(20) default 'EXCELLENT'::character varying
        constraint books_condition_check
            check ((condition)::text = ANY
                   ((ARRAY ['EXCELLENT'::character varying, 'GOOD'::character varying, 'BAD'::character varying])::text[])),
    genre     varchar(255) not null,
    name      varchar(255) not null
);

alter table books
    owner to postgres;

create table library
(
    id          bigint generated by default as identity
        primary key,
    address     varchar(255) not null,
    city        varchar(255) not null,
    library_uid uuid         not null,
    name        varchar(80)  not null
);

alter table library
    owner to postgres;

create table library_books
(
    library_id bigint not null
        constraint fkcwjjd1vlba8vwjrjkvyaaomqw
            references library,
    books_id   bigint not null
        constraint fkeo1ktjs3o74wulkggmk3l5kpg
            references books
);

alter table library_books
    owner to postgres;

create table rating
(
    id       bigint generated by default as identity
        primary key,
    stars    integer     not null
        constraint rating_stars_check
            check ((stars >= 0) AND (stars <= 100)),
    username varchar(80) not null
);

alter table rating
    owner to postgres;

create table reservation
(
    id              bigint generated by default as identity
        primary key,
    book_uid        uuid        not null,
    library_uid     uuid        not null,
    reservation_uid uuid        not null,
    start_date      timestamp   not null,
    status          varchar(20) not null
        constraint reservation_status_check
            check ((status)::text = ANY
                   ((ARRAY ['RENTED'::character varying, 'RETURNED'::character varying, 'EXPIRED'::character varying])::text[])),
    till_date       timestamp   not null,
    username        varchar(80) not null
);

alter table reservation
    owner to postgres;

INSERT INTO library (address, city, library_uid, name) VALUES
    ('2-я Бауманская ул., д.5, стр.1', 'Москва', '83575e12-7ce0-48ee-9931-51919ff3c9ee', 'Библиотека имени 7 Непьющих');

INSERT INTO books (author, book_uid, condition, genre, name) VALUES
    ('Бьерн Страуструп', 'f7cdc58f-2caf-4b15-9727-f89dcc629b27', 'EXCELLENT', 'Научная фантастика', 'Краткий курс C++ в 7 томах');

INSERT INTO library_books (library_id, books_id) VALUES (1, 1);