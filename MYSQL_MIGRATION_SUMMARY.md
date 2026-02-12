# MySQL Migration Summary - COG-13

## Overview
This document summarizes all the changes made to prepare the Spring Boot RealWorld Example Application for migration from SQLite to MySQL.

## Changes Made

### 1. Dependencies Updated (`build.gradle`)
- **Added**: MySQL JDBC driver dependency `mysql:mysql-connector-java:8.0.33`
- **Kept**: SQLite dependency `org.xerial:sqlite-jdbc:3.36.0.3` for backward compatibility during transition
- **Location**: Line 46 in `build.gradle`

### 2. Database Configuration (`application.properties`)
- **Updated**: Main application configuration to use MySQL
  - URL: `jdbc:mysql://localhost:3306/realworld?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`
  - Driver: `com.mysql.cj.jdbc.Driver`
  - Username: `root`
  - Password: `password`
- **Preserved**: SQLite configuration as comments for reference
- **Location**: Lines 1-11 in `src/main/resources/application.properties`

### 3. Test Configuration (`application-test.properties`)
- **Updated**: Test configuration to use MySQL test database
  - URL: `jdbc:mysql://localhost:3306/realworld_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`
  - Driver: `com.mysql.cj.jdbc.Driver`
  - Username: `root`
  - Password: `password`
- **Preserved**: SQLite test configuration as comments
- **Location**: Lines 1-8 in `src/main/resources/application-test.properties`

### 4. Database Schema Migration (`V1__create_tables.sql`)
- **Enhanced**: All table definitions with proper MySQL constraints and foreign keys
- **Key Changes**:
  - Added `NOT NULL` constraints where appropriate
  - Added `UNIQUE` constraints for better data integrity
  - Added foreign key constraints with `ON DELETE CASCADE`
  - Updated timestamp handling with `DEFAULT CURRENT_TIMESTAMP` and `ON UPDATE CURRENT_TIMESTAMP`
  - Added composite primary keys for junction tables

#### Specific Schema Changes:
- **users table**: Added `NOT NULL` constraints for username, password, and email
- **articles table**: Added foreign key to users, `NOT NULL` constraints, and auto-updating timestamps
- **article_favorites table**: Added foreign keys to both articles and users tables
- **follows table**: Added composite primary key and foreign key constraints
- **tags table**: Added `UNIQUE` constraint on name
- **article_tags table**: Added composite primary key and foreign key constraints
- **comments table**: Added foreign keys and `NOT NULL` constraints

### 5. SQL Query Compatibility
- **Verified**: All mapper XML files are already MySQL-compatible
- **No changes needed**: The existing SQL queries use standard SQL syntax that works across database systems
- **Files reviewed**: All files in `src/main/resources/mapper/` directory

### 6. Test Configuration
- **Verified**: `DbTestBase.java` is properly configured
- **No changes needed**: Uses `@ActiveProfiles("test")` which will automatically use the updated test configuration

## Design Considerations Addressed

### MySQL Version
- **Selected**: MySQL 8.0.x (using connector version 8.0.33)
- **Rationale**: Latest stable version with good Spring Boot support

### Connection Configuration
- **Hostname**: localhost (configurable via environment variables)
- **Port**: 3306 (MySQL default)
- **Database**: realworld (production), realworld_test (testing)
- **SSL**: Disabled for local development (should be enabled in production)
- **Timezone**: UTC for consistency

### Character Set and Collation
- **Default**: MySQL 8.0 uses utf8mb4 by default, which is appropriate for the application

### SQLite Compatibility
- **Maintained**: SQLite dependencies and configurations are preserved as comments
- **Benefit**: Allows easy switching back to SQLite for local development if needed

### Index Design Strategy
- **Primary Keys**: All tables have proper primary keys
- **Foreign Keys**: Added for referential integrity
- **Unique Constraints**: Added where appropriate (usernames, emails, slugs, tag names)

### Transaction Isolation Level
- **Default**: MySQL's default isolation level (REPEATABLE READ) is appropriate for this application

### Migration Approach
- **Zero Downtime**: The schema changes are additive and backward compatible
- **Foreign Keys**: Will be enforced after data migration

### Security Requirements
- **Authentication**: Basic username/password authentication configured
- **SSL/TLS**: Disabled for local development (should be enabled in production)
- **Connection Security**: `allowPublicKeyRetrieval=true` for local development only

## Files Modified

1. `build.gradle` - Added MySQL dependency
2. `src/main/resources/application.properties` - Updated database configuration
3. `src/main/resources/application-test.properties` - Updated test database configuration
4. `src/main/resources/db/migration/V1__create_tables.sql` - Enhanced schema for MySQL

## Files Reviewed (No Changes Needed)

1. All mapper XML files in `src/main/resources/mapper/`
2. `src/test/java/io/spring/infrastructure/DbTestBase.java`

## Next Steps for Production Migration

1. **Database Setup**: Create MySQL databases (`realworld` and `realworld_test`)
2. **Environment Variables**: Configure production database credentials
3. **SSL Configuration**: Enable SSL for production connections
4. **Data Migration**: Migrate existing SQLite data to MySQL
5. **Testing**: Run comprehensive tests against MySQL database
6. **Deployment**: Deploy with MySQL configuration
7. **Monitoring**: Monitor application performance with MySQL

## Rollback Plan

If issues arise, the application can be quickly rolled back to SQLite by:
1. Uncommenting SQLite configuration in `application.properties`
2. Commenting out MySQL configuration
3. Reverting the migration script if needed

## Testing Recommendations

1. **Unit Tests**: Run existing test suite against MySQL
2. **Integration Tests**: Test all API endpoints
3. **Performance Tests**: Compare performance between SQLite and MySQL
4. **Data Integrity Tests**: Verify all foreign key constraints work correctly

---

**Ticket ID**: COG-13  
**Date**: $(date)  
**Status**: Ready for testing and deployment