FROM redis:7

# 升级到 Redis 8.10（已内置 RediSearch 模块）
RUN apt-get update && apt-get install -y curl gnupg lsb-release && \
    curl -fsSL https://packages.redis.io/gpg | gpg --dearmor -o /usr/share/keyrings/redis-archive-keyring.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/redis-archive-keyring.gpg] https://packages.redis.io/deb $(lsb_release -cs) main" \
    > /etc/apt/sources.list.d/redis.list && \
    apt-get update && \
    apt-get install -y redis-server redis-tools && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# 使用 apt 安装的 Redis 8.10（替代镜像自带的 Redis 7.4）
CMD ["/usr/bin/redis-server", "--loadmodule", "/usr/lib/redis/modules/redisearch.so"]
