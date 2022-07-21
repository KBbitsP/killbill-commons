/*
 * Copyright 2004-2014 Brian McCallister
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.skife.jdbi.v2.docs;

import static java.util.Arrays.asList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.skife.jdbi.v2.ContainerBuilder;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.JDBIQuarantineTests;
import org.skife.jdbi.v2.logging.PrintStreamLog;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterContainerMapper;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.tweak.ContainerFactory;
import org.skife.jdbi.v2.unstable.BindIn;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@Category(JDBIQuarantineTests.class) // Feature disabled
public class TestInClauseExpansion
{
    private DBI    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        dbi = new DBI("jdbc:h2:mem:" + UUID.randomUUID());

        dbi.setSQLLog(new PrintStreamLog(System.out));
        handle = dbi.open();
        handle.execute("create table something( id integer primary key, name varchar(100) )");
    }

    @After
    public void tearDown() throws Exception
    {
        handle.close();
    }

    @Test
    public void testInClauseExpansion() throws Exception
    {
        handle.execute("insert into something (name, id) values ('Brian', 1), ('Jeff', 2), ('Tom', 3)");

        DAO dao = handle.attach(DAO.class);

        assertThat(dao.findIdsForNames(asList(1, 2)), equalTo(Set.of("Brian", "Jeff")));
    }

    @UseStringTemplate3StatementLocator
    @RegisterContainerMapper(SetContainerFactory.class)
    public static interface DAO
    {
        @SqlQuery
        public Set<String> findIdsForNames(@BindIn("names") List<Integer> names);
    }

    public static class SetContainerFactory implements ContainerFactory<Set>
    {

        @Override
        public boolean accepts(Class<?> type)
        {
            return Set.class.isAssignableFrom(type);
        }

        @Override
        public ContainerBuilder<Set> newContainerBuilderFor(Class<?> type)
        {
            return new ContainerBuilder<Set>()
            {
                final Set<Object> builder = new HashSet<>();

                @Override
                public ContainerBuilder<Set> add(Object it)
                {
                    builder.add(it);
                    return this;
                }

                @Override
                public Set<Object> build()
                {
                    return Set.copyOf(builder);
                }
            };
        }
    }
}
