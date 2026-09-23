-- TeddyCoin: минимальная таблица для облачного профиля и онлайн-рейтинга.
-- Выполни этот SQL в Supabase SQL Editor.
-- В приложение вставляй только publishable/anon key, НЕ service_role.

create table if not exists public.players (
  user_id text primary key,
  display_name text not null,
  coins bigint not null default 0,
  level integer not null default 1,
  taps integer not null default 0,
  updated_at timestamptz not null default now()
);

alter table public.players enable row level security;

-- Для простой первой версии игры разрешаем чтение рейтинга и upsert.
-- Позже эти политики стоит усилить через Supabase Auth.
drop policy if exists "public read players" on public.players;
create policy "public read players"
on public.players for select
to anon
using (true);

drop policy if exists "public insert players" on public.players;
create policy "public insert players"
on public.players for insert
to anon
with check (true);

drop policy if exists "public update players" on public.players;
create policy "public update players"
on public.players for update
to anon
using (true)
with check (true);
