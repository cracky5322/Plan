/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.system.database.databases.sql;

import com.djrapitops.plan.api.exceptions.EnableException;
import com.djrapitops.plan.api.exceptions.database.DBInitException;
import com.djrapitops.plan.data.store.containers.ServerContainer;
import com.djrapitops.plan.data.store.keys.ServerKeys;
import com.djrapitops.plan.system.PlanSystem;
import com.djrapitops.plan.system.database.databases.DBType;
import com.djrapitops.plan.system.locale.Locale;
import com.djrapitops.plan.system.settings.config.PlanConfig;
import com.djrapitops.plan.system.settings.paths.DatabaseSettings;
import com.djrapitops.plan.system.settings.paths.WebserverSettings;
import com.djrapitops.plugin.logging.L;
import com.djrapitops.plugin.logging.console.TestPluginLogger;
import com.djrapitops.plugin.logging.error.ErrorHandler;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import rules.PluginComponentMocker;
import utilities.CIProperties;
import utilities.OptionalAssert;
import utilities.RandomData;
import utilities.TestConstants;

import static org.junit.Assume.assumeTrue;

/**
 * Test for the patching of Plan 4.5.2 MySQL DB into the newest schema.
 *
 * @author Rsl1122
 */
public class DBPatchMySQLRegressionTest extends DBPatchRegressionTest {

    private static final int TEST_PORT_NUMBER = RandomData.randomInt(9005, 9500);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public PluginComponentMocker component = new PluginComponentMocker(temporaryFolder);

    String serverTable = "CREATE TABLE IF NOT EXISTS plan_servers (id integer NOT NULL AUTO_INCREMENT, uuid varchar(36) NOT NULL UNIQUE, name varchar(100), web_address varchar(100), is_installed boolean NOT NULL DEFAULT 1, max_players integer NOT NULL DEFAULT -1, PRIMARY KEY (id))";
    String usersTable = "CREATE TABLE IF NOT EXISTS plan_users (id integer NOT NULL AUTO_INCREMENT, uuid varchar(36) NOT NULL UNIQUE, registered bigint NOT NULL, name varchar(16) NOT NULL, times_kicked integer NOT NULL DEFAULT 0, PRIMARY KEY (id))";
    String userInfoTable = "CREATE TABLE IF NOT EXISTS plan_user_info (user_id integer NOT NULL, registered bigint NOT NULL, opped boolean NOT NULL DEFAULT 0, banned boolean NOT NULL DEFAULT 0, server_id integer NOT NULL, FOREIGN KEY(user_id) REFERENCES plan_users(id), FOREIGN KEY(server_id) REFERENCES plan_servers(id))";
    String geoInfoTable = "CREATE TABLE IF NOT EXISTS plan_ips (user_id integer NOT NULL, ip varchar(39) NOT NULL, geolocation varchar(50) NOT NULL, ip_hash varchar(200), last_used bigint NOT NULL DEFAULT 0, FOREIGN KEY(user_id) REFERENCES plan_users(id))";
    String nicknameTable = "CREATE TABLE IF NOT EXISTS plan_nicknames (user_id integer NOT NULL, nickname varchar(75) NOT NULL, server_id integer NOT NULL, last_used bigint NOT NULL, FOREIGN KEY(user_id) REFERENCES plan_users(id), FOREIGN KEY(server_id) REFERENCES plan_servers(id))";
    String sessionsTable = "CREATE TABLE IF NOT EXISTS plan_sessions (id integer NOT NULL AUTO_INCREMENT, user_id integer NOT NULL, server_id integer NOT NULL, session_start bigint NOT NULL, session_end bigint NOT NULL, mob_kills integer NOT NULL, deaths integer NOT NULL, afk_time bigint NOT NULL, FOREIGN KEY(user_id) REFERENCES plan_users(id), FOREIGN KEY(server_id) REFERENCES plan_servers(id), PRIMARY KEY (id))";
    String killsTable = "CREATE TABLE IF NOT EXISTS plan_kills (killer_id integer NOT NULL, victim_id integer NOT NULL, server_id integer NOT NULL, weapon varchar(30) NOT NULL, date bigint NOT NULL, session_id integer NOT NULL, FOREIGN KEY(killer_id) REFERENCES plan_users(id), FOREIGN KEY(victim_id) REFERENCES plan_users(id), FOREIGN KEY(session_id) REFERENCES plan_sessions(id), FOREIGN KEY(server_id) REFERENCES plan_servers(id))";
    String pingTable = "CREATE TABLE IF NOT EXISTS plan_ping (id integer NOT NULL AUTO_INCREMENT, user_id integer NOT NULL, server_id integer NOT NULL, date bigint NOT NULL, max_ping integer NOT NULL, min_ping integer NOT NULL, avg_ping double NOT NULL, PRIMARY KEY (id), FOREIGN KEY(user_id) REFERENCES plan_users(id), FOREIGN KEY(server_id) REFERENCES plan_servers(id))";
    String commandUseTable = "CREATE TABLE IF NOT EXISTS plan_commandusages (id integer NOT NULL AUTO_INCREMENT, command varchar(20) NOT NULL, times_used integer NOT NULL, server_id integer NOT NULL, PRIMARY KEY (id), FOREIGN KEY(server_id) REFERENCES plan_servers(id))";
    String tpsTable = "CREATE TABLE IF NOT EXISTS plan_tps (server_id integer NOT NULL, date bigint NOT NULL, tps double NOT NULL, players_online integer NOT NULL, cpu_usage double NOT NULL, ram_usage bigint NOT NULL, entities integer NOT NULL, chunks_loaded integer NOT NULL, free_disk_space bigint NOT NULL, FOREIGN KEY(server_id) REFERENCES plan_servers(id))";
    String worldsTable = "CREATE TABLE IF NOT EXISTS plan_worlds (id integer NOT NULL AUTO_INCREMENT, world_name varchar(100) NOT NULL, server_id integer NOT NULL, PRIMARY KEY (id), FOREIGN KEY(server_id) REFERENCES plan_servers(id))";
    String worldTimesTable = "CREATE TABLE IF NOT EXISTS plan_world_times (user_id integer NOT NULL, world_id integer NOT NULL, server_id integer NOT NULL, session_id integer NOT NULL, survival_time bigint NOT NULL DEFAULT 0, creative_time bigint NOT NULL DEFAULT 0, adventure_time bigint NOT NULL DEFAULT 0, spectator_time bigint NOT NULL DEFAULT 0, FOREIGN KEY(user_id) REFERENCES plan_users(id), FOREIGN KEY(world_id) REFERENCES plan_worlds(id), FOREIGN KEY(server_id) REFERENCES plan_servers(id), FOREIGN KEY(session_id) REFERENCES plan_sessions(id))";
    String securityTable = "CREATE TABLE IF NOT EXISTS plan_security (username varchar(100) NOT NULL UNIQUE, salted_pass_hash varchar(100) NOT NULL UNIQUE, permission_level integer NOT NULL)";
    String transferTable = "CREATE TABLE IF NOT EXISTS plan_transfer (sender_server_id integer NOT NULL, expiry_date bigint NOT NULL DEFAULT 0, type varchar(100) NOT NULL, extra_variables varchar(255) DEFAULT '', content_64 MEDIUMTEXT, part bigint NOT NULL DEFAULT 0, FOREIGN KEY(sender_server_id) REFERENCES plan_servers(id))";

    private MySQLDB underTest;

    @BeforeClass
    public static void ensureTravisInUse() {
        boolean isTravis = Boolean.parseBoolean(System.getenv(CIProperties.IS_TRAVIS));
        assumeTrue(isTravis);
    }

    @Before
    public void setUpDBWithOldSchema() throws DBInitException, EnableException {
        PlanSystem system = component.getPlanSystem();
        PlanConfig config = system.getConfigSystem().getConfig();
        config.set(DatabaseSettings.MYSQL_DATABASE, "Plan");
        config.set(DatabaseSettings.MYSQL_USER, "travis");
        config.set(DatabaseSettings.MYSQL_PASS, "");
        config.set(DatabaseSettings.MYSQL_HOST, "127.0.0.1");
        config.set(DatabaseSettings.TYPE, "MySQL");
        config.set(WebserverSettings.PORT, TEST_PORT_NUMBER);

        system.enable();

        underTest = (MySQLDB) system.getDatabaseSystem().getActiveDatabaseByName(DBType.MYSQL.getName());

        underTest.setOpen(true);
        underTest.setupDataSource();

        dropAllTables();

        // Initialize database with the old table schema
        underTest.execute(serverTable);
        underTest.execute(usersTable);
        underTest.execute(userInfoTable);
        underTest.execute(geoInfoTable);
        underTest.execute(nicknameTable);
        underTest.execute(sessionsTable);
        underTest.execute(killsTable);
        underTest.execute(pingTable);
        underTest.execute(commandUseTable);
        underTest.execute(tpsTable);
        underTest.execute(worldsTable);
        underTest.execute(worldTimesTable);
        underTest.execute(securityTable);
        underTest.execute(transferTable);

        underTest.createTables();

        insertData(underTest);
    }

    private void dropAllTables() {
        underTest.execute("DROP DATABASE Plan");
        underTest.execute("CREATE DATABASE Plan");
        underTest.execute("USE Plan");
    }

    @After
    public void closeDatabase() {
        underTest.close();
    }

    @Test
    public void mysqlPatchTaskWorksWithoutErrors() {
        PatchTask patchTask = new PatchTask(underTest.patches(), new Locale(), new TestPluginLogger(), new ErrorHandler() {
            @Override
            public void log(L l, Class aClass, Throwable throwable) {
                throw new AssertionError(throwable);
            }
        });

        // Patching might fail due to exception.
        patchTask.run();

        assertPatchesHaveBeenApplied(underTest);

        // Make sure that a fetch works.
        ServerContainer server = underTest.fetch().getServerContainer(TestConstants.SERVER_UUID);
        OptionalAssert.equals(1, server.getValue(ServerKeys.PLAYER_KILL_COUNT));

        // Make sure no foreign key checks fail on removal
        underTest.remove().everything();
    }
}