### Dynamic Database Source Implementation Guidance

Dynamic database source switching is a powerful technique to manage multiple database connections within a single application, allowing you to route database operations to different instances based on runtime conditions. This is crucial for scenarios like read/write separation, multi-tenancy, or sharding.

Your project already employs a robust mechanism for this through the `DsContextHolder` and the conceptual use of Spring's `AbstractRoutingDataSource`.

#### Why Implement Dynamic Database Sources?

1.  **Read/Write Separation (Master-Slave Architecture):**
    *   **Why:** This is the most common reason. Read-heavy applications can significantly improve performance and scalability by directing all read operations to one or more "slave" databases, while all write operations go to a single "master" database. This offloads the master, reduces contention, and allows for independent scaling of read and write capacities.
    *   **Benefit:** Reduced load on the master database, faster read queries, and improved overall system throughput.

2.  **Multi-Tenancy:**
    *   **Why:** In applications serving multiple clients or organizations (tenants), each tenant might require its own isolated database for data security, compliance, or performance reasons. Dynamic switching allows the application to connect to the correct tenant's database based on the authenticated user or tenant context.
    *   **Benefit:** Data isolation between tenants, enhanced security, and simplified application logic for handling tenant-specific data.

3.  **Database Sharding:**
    *   **Why:** For extremely large datasets, a single database might become a bottleneck. Sharding involves horizontally partitioning data across multiple database instances. Dynamic data source switching can route queries to the correct shard based on a sharding key (e.g., user ID, region).
    *   **Benefit:** Enables handling massive amounts of data and high transaction volumes by distributing the load across multiple database servers.

4.  **Performance Optimization & High Availability:**
    *   **Why:** By intelligently routing queries, you can optimize resource utilization. In a master-slave setup, if the master goes down, read operations can still continue against the slaves, providing a degree of high availability for read-only functionalities.
    *   **Benefit:** Improved application responsiveness and resilience against single points of failure.

#### How to Implement Dynamic Database Sources (Using Your Project's Pattern)

The core idea is to use a `ThreadLocal` to store the desired data source for the current execution thread and then use Spring's `AbstractRoutingDataSource` to pick the actual connection.

**1. Define Your Data Sources:**

First, you need to configure all your database connections (master, slave, tenant-specific, etc.) in your Spring application. This typically involves defining `DataSource` beans in your configuration.

```java
// Example Spring Configuration (e.g., DataSourceConfig.java)
@Configuration
public class DataSourceConfig {

    @Bean(name = "masterDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "slaveDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.slave")
    public DataSource slaveDataSource() {
        return DataSourceBuilder.create().build();
    }

    // ... other data sources
}
```

**2. Implement `AbstractRoutingDataSource`:**

This is the central component that will decide which data source to use at runtime. You'll extend `AbstractRoutingDataSource` and override the `determineCurrentLookupKey()` method.

```java
// Example DynamicRoutingDataSource.java
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        // This is where DsContextHolder comes into play!
        return DsContextHolder.get();
    }
}
```

**3. Configure the Dynamic Data Source:**

In your Spring configuration, you'll set up the `DynamicRoutingDataSource` and map your named data sources to it.

```java
// Example Spring Configuration (e.g., DataSourceConfig.java continued)
@Bean(name = "dynamicDataSource")
public DataSource dynamicDataSource(@Qualifier("masterDataSource") DataSource masterDataSource,
                                    @Qualifier("slaveDataSource") DataSource slaveDataSource) {
    DynamicRoutingDataSource dynamicRoutingDataSource = new DynamicRoutingDataSource();

    Map<Object, Object> targetDataSources = new HashMap<>();
    targetDataSources.put(MasterSlaveDsEnum.MASTER.name().toUpperCase(), masterDataSource);
    targetDataSources.put(MasterSlaveDsEnum.SLAVE.name().toUpperCase(), slaveDataSource);
    // ... add other data sources

    dynamicRoutingDataSource.setTargetDataSources(targetDataSources);
    dynamicRoutingDataSource.setDefaultTargetDataSource(masterDataSource); // Set a default
    return dynamicRoutingDataSource;
}

// Configure your SqlSessionFactory to use the dynamicDataSource
@Bean
public SqlSessionFactory sqlSessionFactory(@Qualifier("dynamicDataSource") DataSource dynamicDataSource) throws Exception {
    MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
    factoryBean.setDataSource(dynamicDataSource);
    // ... other Mybatis configurations
    return factoryBean.getObject();
}
```

**4. Utilize `DsContextHolder` for Runtime Switching:**

This is where your `DsContextHolder` comes into play. You'll use it to set the desired data source type for the current thread. The `DsContextHolder`'s `ThreadLocal` ensures that this setting is specific to the current thread.

*   **`DsContextHolder.set(String dbType)`:** Pushes a new data source onto the thread's context stack.
*   **`DsContextHolder.reset()`:** Pops the current data source from the stack, reverting to the previous one if any.

**5. Aspect-Oriented Programming (AOP) for Automatic Switching:**

The most elegant way to manage `DsContextHolder.set()` and `DsContextHolder.reset()` is through AOP. You can define custom annotations (e.g., `@Master`, `@Slave`, `@TenantDataSource("tenantId")`) and create an aspect that intercepts method calls.

```java
// Example Custom Annotation
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UseDataSource {
    MasterSlaveDsEnum value() default MasterSlaveDsEnum.MASTER;
}

// Example Aspect
@Aspect
@Component
public class DataSourceAspect {

    @Around("@annotation(useDataSource)")
    public Object switchDataSource(ProceedingJoinPoint joinPoint, UseDataSource useDataSource) throws Throwable {
        try {
            DsContextHolder.set(useDataSource.value()); // Set the desired data source
            return joinPoint.proceed();
        } finally {
            DsContextHolder.reset(); // Always reset to clean up the thread context
        }
    }
}
```

Then, you can simply annotate your service methods:

```java
@Service
public class MyService {

    @UseDataSource(MasterSlaveDsEnum.SLAVE)
    public List<Data> readOnlyOperation() {
        // This method will use the slave data source
        return myMapper.findAll();
    }

    @UseDataSource(MasterSlaveDsEnum.MASTER)
    @Transactional // Transactions typically require a single data source
    public void writeOperation(Data data) {
        // This method will use the master data source
        myMapper.save(data);
    }
}
```

**Important Considerations:**

*   **Transactions:** As noted in `DsContextHolder`, **do not cross data sources within a single transaction.** If a method is marked `@Transactional`, ensure it consistently uses one data source (e.g., master for writes). Switching data sources mid-transaction can lead to data inconsistency and errors.
*   **`InheritableThreadLocal`:** The use of `InheritableThreadLocal` in `DsContextHolder` is beneficial if you have asynchronous operations (e.g., using `CompletableFuture` or `ExecutorService`) where child threads need to inherit the data source context from their parent.
*   **Cleanup:** Always ensure `DsContextHolder.reset()` is called in a `finally` block to prevent thread context leakage, which can lead to incorrect data source usage in subsequent operations on the same thread.
*   **Default Data Source:** Always configure a `defaultTargetDataSource` for `AbstractRoutingDataSource` to handle cases where no specific data source is set in the `DsContextHolder`.

By following this pattern, you can effectively implement dynamic database source switching, making your application more scalable, performant, and adaptable to various database architectures.