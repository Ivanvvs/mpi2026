UPDATE user_profiles up
SET contact_info = au.email
FROM app_users au
WHERE up.account_id = au.id
  AND au.email IS NOT NULL
  AND (up.contact_info IS NULL OR btrim(up.contact_info) = '');
