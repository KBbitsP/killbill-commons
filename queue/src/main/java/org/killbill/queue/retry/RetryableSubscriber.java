/*
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

package org.killbill.queue.retry;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.joda.time.DateTime;
import org.killbill.bus.api.BusEvent;
import org.killbill.clock.Clock;
import org.killbill.commons.utils.cache.Cache;
import org.killbill.commons.utils.cache.DefaultSynchronizedCache;
import org.killbill.notificationq.api.NotificationEvent;
import org.killbill.notificationq.api.NotificationQueueService.NotificationQueueHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryableSubscriber extends RetryableHandler {

    private static final Logger log = LoggerFactory.getLogger(RetryableSubscriber.class);

    public RetryableSubscriber(final Clock clock,
                               final RetryableService retryableService,
                               final NotificationQueueHandler handlerDelegate) {
        super(clock, retryableService, handlerDelegate);
    }

    public void handleEvent(final BusEvent event) {
        handleReadyNotification(new SubscriberNotificationEvent(event, event.getClass()),
                                clock.getUTCNow(),
                                event.getUserToken(),
                                event.getSearchKey1(),
                                event.getSearchKey2());
    }

    public interface SubscriberAction<T extends BusEvent> {

        void run(T event);
    }

    public static final class SubscriberQueueHandler implements NotificationQueueHandler {

        // Similar to org.killbill.commons.eventbus.SubscriberRegistry
        private static final Cache<Class<?>, Set<Class<?>>> FLATTEN_HIERARCHY_CACHE = new DefaultSynchronizedCache<>(
                // LinkedHashSet to make sure maintains its order. See
                // org.killbill.commons.eventbus.SubscriberRegistry comments (on line ~204) to find out more why.
                key -> new LinkedHashSet<>(org.killbill.commons.utils.TypeToken.getRawTypes(key)));

        private final Map<Class<?>, SubscriberAction<? extends BusEvent>> actions = new HashMap<>();

        public SubscriberQueueHandler() {
        }

        public <B extends BusEvent> void subscribe(final Class<B> busEventClass, final SubscriberAction<B> action) {
            actions.put(busEventClass, action);
        }

        @Override
        public void handleReadyNotification(final NotificationEvent eventJson, final DateTime eventDateTime, final UUID userToken, final Long searchKey1, final Long searchKey2) {
            if (!(eventJson instanceof SubscriberNotificationEvent)) {
                log.error("SubscriberQueueHandler received an unexpected event className='{}'", eventJson.getClass());
            } else {
                final BusEvent busEvent = ((SubscriberNotificationEvent) eventJson).getBusEvent();

                final Set<Class<?>> eventTypes = FLATTEN_HIERARCHY_CACHE.get(busEvent.getClass());
                for (final Class<?> eventType : eventTypes) {
                    final SubscriberAction<BusEvent> next = (SubscriberAction<BusEvent>) actions.get(eventType);
                    if (next != null) {
                        next.run(busEvent);
                    }
                }
            }
        }
    }
}
