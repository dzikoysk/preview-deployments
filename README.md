# Feature Preview

### Notes

Increase `server_names_hash` in `/etc/nginx/nginx.conf` to properly handle long server names:

```
server_names_hash_bucket_size  256;
```
