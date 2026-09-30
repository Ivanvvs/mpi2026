ALTER TABLE user_profiles
    ADD CONSTRAINT uk_user_profiles_account UNIQUE (account_id);
