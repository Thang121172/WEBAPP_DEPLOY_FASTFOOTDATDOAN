import os, sys
from pathlib import Path
BASE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BASE_DIR))
os.environ.setdefault('DJANGO_SETTINGS_MODULE','core.settings.dev')
import django
django.setup()
from django.db import connection

sql_statements = [
    # Create auth_user if missing
    """
    CREATE TABLE IF NOT EXISTS auth_user (
        id bigint PRIMARY KEY,
        password varchar(128) NOT NULL,
        last_login timestamptz NULL,
        is_superuser boolean NOT NULL,
        username varchar(150) NOT NULL,
        first_name varchar(150) NOT NULL,
        last_name varchar(150) NOT NULL,
        email varchar(254) NOT NULL,
        is_staff boolean NOT NULL,
        is_active boolean NOT NULL,
        date_joined timestamptz NOT NULL
    );
    """,
    # create sequence for auth_user id if not exists and set default
    """
    DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relkind='S' AND relname='auth_user_id_seq') THEN
            CREATE SEQUENCE auth_user_id_seq;
        END IF;
        -- set default for id column
        ALTER TABLE auth_user ALTER COLUMN id SET DEFAULT nextval('auth_user_id_seq');
    EXCEPTION WHEN undefined_table THEN
        -- ignore
        RAISE NOTICE 'auth_user or sequence not present yet';
    END $$;
    """,
    # create auth_user_groups
    """
    CREATE TABLE IF NOT EXISTS auth_user_groups (
        id serial PRIMARY KEY,
        user_id bigint NOT NULL,
        group_id integer NOT NULL,
        CONSTRAINT auth_user_groups_user_id_fk FOREIGN KEY (user_id) REFERENCES auth_user(id) ON DELETE CASCADE,
        CONSTRAINT auth_user_groups_group_id_fk FOREIGN KEY (group_id) REFERENCES auth_group(id) ON DELETE CASCADE
    );
    """,
    # create auth_user_user_permissions
    """
    CREATE TABLE IF NOT EXISTS auth_user_user_permissions (
        id serial PRIMARY KEY,
        user_id bigint NOT NULL,
        permission_id integer NOT NULL,
        CONSTRAINT auth_user_user_permissions_user_id_fk FOREIGN KEY (user_id) REFERENCES auth_user(id) ON DELETE CASCADE,
        CONSTRAINT auth_user_user_permissions_permission_id_fk FOREIGN KEY (permission_id) REFERENCES auth_permission(id) ON DELETE CASCADE
    );
    """,
    # Copy data from accounts_user into auth_user if auth_user empty
    """
    INSERT INTO auth_user (id, password, last_login, is_superuser, username, first_name, last_name, email, is_staff, is_active, date_joined)
    SELECT id, password, last_login, is_superuser, username, first_name, last_name, email, is_staff, is_active, date_joined
    FROM accounts_user
    WHERE NOT EXISTS (SELECT 1 FROM auth_user LIMIT 1);
    """,
    # Copy groups and permissions mappings if target tables empty
    """
    INSERT INTO auth_user_groups (user_id, group_id)
    SELECT user_id, group_id FROM accounts_user_groups
    WHERE NOT EXISTS (SELECT 1 FROM auth_user_groups LIMIT 1);
    """,
    """
    INSERT INTO auth_user_user_permissions (user_id, permission_id)
    SELECT user_id, permission_id FROM accounts_user_user_permissions
    WHERE NOT EXISTS (SELECT 1 FROM auth_user_user_permissions LIMIT 1);
    """,
    # Set sequence value to max(id)+1
    """
    SELECT setval('auth_user_id_seq', COALESCE((SELECT MAX(id) FROM auth_user), 1));
    """,
]

with connection.cursor() as cur:
    for s in sql_statements:
        try:
            cur.execute(s)
            print('Executed statement chunk')
        except Exception as e:
            print('Error executing chunk:', e)

print('Done')
