/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.apache.gravitino.authorization;

import static com.apache.gravitino.Configs.SERVICE_ADMINS;

import com.apache.gravitino.Config;
import com.apache.gravitino.EntityStore;
import com.apache.gravitino.GravitinoEnv;
import com.apache.gravitino.StringIdentifier;
import com.apache.gravitino.exceptions.GroupAlreadyExistsException;
import com.apache.gravitino.exceptions.NoSuchGroupException;
import com.apache.gravitino.exceptions.NoSuchMetalakeException;
import com.apache.gravitino.exceptions.NoSuchRoleException;
import com.apache.gravitino.exceptions.NoSuchUserException;
import com.apache.gravitino.exceptions.RoleAlreadyExistsException;
import com.apache.gravitino.exceptions.UserAlreadyExistsException;
import com.apache.gravitino.meta.AuditInfo;
import com.apache.gravitino.meta.BaseMetalake;
import com.apache.gravitino.meta.SchemaVersion;
import com.apache.gravitino.storage.RandomIdGenerator;
import com.apache.gravitino.storage.memory.TestMemoryEntityStore;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestAccessControlManager {

  private static AccessControlManager accessControlManager;

  private static EntityStore entityStore;

  private static Config config;

  private static String METALAKE = "metalake";

  private static BaseMetalake metalakeEntity =
      BaseMetalake.builder()
          .withId(1L)
          .withName(METALAKE)
          .withAuditInfo(
              AuditInfo.builder().withCreator("test").withCreateTime(Instant.now()).build())
          .withVersion(SchemaVersion.V_0_1)
          .build();

  @BeforeAll
  public static void setUp() throws Exception {
    config = new Config(false) {};
    config.set(SERVICE_ADMINS, Lists.newArrayList("admin1", "admin2"));

    entityStore = new TestMemoryEntityStore.InMemoryEntityStore();
    entityStore.initialize(config);
    entityStore.setSerDe(null);

    entityStore.put(metalakeEntity, true);

    accessControlManager = new AccessControlManager(entityStore, new RandomIdGenerator(), config);
    FieldUtils.writeField(GravitinoEnv.getInstance(), "entityStore", entityStore, true);
    FieldUtils.writeField(
        GravitinoEnv.getInstance(), "accessControlManager", accessControlManager, true);
  }

  @AfterAll
  public static void tearDown() throws IOException {
    if (entityStore != null) {
      entityStore.close();
      entityStore = null;
    }
  }

  @Test
  public void testAddUser() {
    User user = accessControlManager.addUser("metalake", "testAdd");
    Assertions.assertEquals("testAdd", user.name());
    Assertions.assertTrue(user.roles().isEmpty());

    user = accessControlManager.addUser("metalake", "testAddWithOptionalField");

    Assertions.assertEquals("testAddWithOptionalField", user.name());
    Assertions.assertTrue(user.roles().isEmpty());

    // Test with NoSuchMetalakeException
    Assertions.assertThrows(
        NoSuchMetalakeException.class, () -> accessControlManager.addUser("no-exist", "testAdd"));

    // Test with UserAlreadyExistsException
    Assertions.assertThrows(
        UserAlreadyExistsException.class,
        () -> accessControlManager.addUser("metalake", "testAdd"));
  }

  @Test
  public void testGetUser() {
    accessControlManager.addUser("metalake", "testGet");

    User user = accessControlManager.getUser("metalake", "testGet");
    Assertions.assertEquals("testGet", user.name());

    // Test with NoSuchMetalakeException
    Assertions.assertThrows(
        NoSuchMetalakeException.class, () -> accessControlManager.addUser("no-exist", "testAdd"));

    // Test to get non-existed user
    Throwable exception =
        Assertions.assertThrows(
            NoSuchUserException.class, () -> accessControlManager.getUser("metalake", "not-exist"));
    Assertions.assertTrue(exception.getMessage().contains("User not-exist does not exist"));
  }

  @Test
  public void testRemoveUser() {
    accessControlManager.addUser("metalake", "testRemove");

    // Test with NoSuchMetalakeException
    Assertions.assertThrows(
        NoSuchMetalakeException.class, () -> accessControlManager.addUser("no-exist", "testAdd"));

    // Test to remove user
    boolean removed = accessControlManager.removeUser("metalake", "testRemove");
    Assertions.assertTrue(removed);

    // Test to remove non-existed user
    boolean removed1 = accessControlManager.removeUser("metalake", "no-exist");
    Assertions.assertFalse(removed1);
  }

  @Test
  public void testAddGroup() {
    Group group = accessControlManager.addGroup("metalake", "testAdd");
    Assertions.assertEquals("testAdd", group.name());
    Assertions.assertTrue(group.roles().isEmpty());

    group = accessControlManager.addGroup("metalake", "testAddWithOptionalField");

    Assertions.assertEquals("testAddWithOptionalField", group.name());
    Assertions.assertTrue(group.roles().isEmpty());

    // Test with NoSuchMetalakeException
    Assertions.assertThrows(
        NoSuchMetalakeException.class, () -> accessControlManager.addUser("no-exist", "testAdd"));

    // Test with GroupAlreadyExistsException
    Assertions.assertThrows(
        GroupAlreadyExistsException.class,
        () -> accessControlManager.addGroup("metalake", "testAdd"));
  }

  @Test
  public void testGetGroup() {
    accessControlManager.addGroup("metalake", "testGet");

    Group group = accessControlManager.getGroup("metalake", "testGet");
    Assertions.assertEquals("testGet", group.name());

    // Test with NoSuchMetalakeException
    Assertions.assertThrows(
        NoSuchMetalakeException.class, () -> accessControlManager.addUser("no-exist", "testAdd"));

    // Test to get non-existed group
    Throwable exception =
        Assertions.assertThrows(
            NoSuchGroupException.class,
            () -> accessControlManager.getGroup("metalake", "not-exist"));
    Assertions.assertTrue(exception.getMessage().contains("Group not-exist does not exist"));
  }

  @Test
  public void testRemoveGroup() {
    accessControlManager.addGroup("metalake", "testRemove");

    // Test with NoSuchMetalakeException
    Assertions.assertThrows(
        NoSuchMetalakeException.class, () -> accessControlManager.addUser("no-exist", "testAdd"));

    // Test to remove group
    boolean removed = accessControlManager.removeGroup("metalake", "testRemove");
    Assertions.assertTrue(removed);

    // Test to remove non-existed group
    boolean removed1 = accessControlManager.removeUser("metalake", "no-exist");
    Assertions.assertFalse(removed1);
  }

  @Test
  public void testMetalakeAdmin() {
    User user = accessControlManager.addMetalakeAdmin("test");
    Assertions.assertEquals("test", user.name());
    Assertions.assertTrue(user.roles().isEmpty());
    Assertions.assertTrue(accessControlManager.isMetalakeAdmin("test"));

    // Test with UserAlreadyExistsException
    Assertions.assertThrows(
        UserAlreadyExistsException.class, () -> accessControlManager.addMetalakeAdmin("test"));

    // Test to remove admin
    boolean removed = accessControlManager.removeMetalakeAdmin("test");
    Assertions.assertTrue(removed);
    Assertions.assertFalse(accessControlManager.isMetalakeAdmin("test"));

    // Test to remove non-existed admin
    boolean removed1 = accessControlManager.removeMetalakeAdmin("no-exist");
    Assertions.assertFalse(removed1);
  }

  @Test
  public void testServiceAdmin() {
    Assertions.assertTrue(accessControlManager.isServiceAdmin("admin1"));
    Assertions.assertTrue(accessControlManager.isServiceAdmin("admin2"));
    Assertions.assertFalse(accessControlManager.isServiceAdmin("admin3"));
  }

  @Test
  public void testCreateRole() {
    Map<String, String> props = ImmutableMap.of("key1", "value1");

    Role role =
        accessControlManager.createRole(
            "metalake",
            "create",
            props,
            Lists.newArrayList(
                SecurableObjects.ofCatalog(
                    "catalog", Lists.newArrayList(Privileges.UseCatalog.allow()))));
    Assertions.assertEquals("create", role.name());
    testProperties(props, role.properties());

    // Test with RoleAlreadyExistsException
    Assertions.assertThrows(
        RoleAlreadyExistsException.class,
        () ->
            accessControlManager.createRole(
                "metalake",
                "create",
                props,
                Lists.newArrayList(
                    SecurableObjects.ofCatalog(
                        "catalog", Lists.newArrayList(Privileges.UseCatalog.allow())))));
  }

  @Test
  public void testLoadRole() {
    Map<String, String> props = ImmutableMap.of("k1", "v1");

    accessControlManager.createRole(
        "metalake",
        "loadRole",
        props,
        Lists.newArrayList(
            SecurableObjects.ofCatalog(
                "catalog", Lists.newArrayList(Privileges.UseCatalog.allow()))));

    Role cachedRole = accessControlManager.getRole("metalake", "loadRole");
    accessControlManager.getRoleManager().getCache().invalidateAll();
    Role role = accessControlManager.getRole("metalake", "loadRole");

    // Verify the cached roleEntity is correct
    Assertions.assertEquals(role, cachedRole);

    Assertions.assertEquals("loadRole", role.name());
    testProperties(props, role.properties());

    // Test load non-existed group
    Throwable exception =
        Assertions.assertThrows(
            NoSuchRoleException.class, () -> accessControlManager.getRole("metalake", "not-exist"));
    Assertions.assertTrue(exception.getMessage().contains("Role not-exist does not exist"));
  }

  @Test
  public void testDropRole() {
    Map<String, String> props = ImmutableMap.of("k1", "v1");

    accessControlManager.createRole(
        "metalake",
        "testDrop",
        props,
        Lists.newArrayList(
            SecurableObjects.ofCatalog(
                "catalog", Lists.newArrayList(Privileges.UseCatalog.allow()))));

    // Test drop role
    boolean dropped = accessControlManager.deleteRole("metalake", "testDrop");
    Assertions.assertTrue(dropped);

    // Test drop non-existed role
    boolean dropped1 = accessControlManager.deleteRole("metalake", "no-exist");
    Assertions.assertFalse(dropped1);
  }

  private void testProperties(Map<String, String> expectedProps, Map<String, String> testProps) {
    expectedProps.forEach(
        (k, v) -> {
          Assertions.assertEquals(v, testProps.get(k));
        });

    Assertions.assertFalse(testProps.containsKey(StringIdentifier.ID_KEY));
  }
}