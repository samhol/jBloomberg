/*
 * Copyright (C) 2012 - present by Yann Le Tallec.
 * Please see distribution for license.
 */
package com.assylias.jbloomberg;

import static com.assylias.jbloomberg.RealtimeField.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.testng.Assert.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class BloombergSessionAsyncSusbcriptionTest {
    private static final Logger LOG = LoggerFactory.getLogger(BloombergSessionAsyncSusbcriptionTest.class);

    private static final int TIMEOUT = 200;
    private static CountDownLatch latch;
    private DefaultBloombergSession session;

    @BeforeClass
    public void beforeClass() {
    }

    @BeforeMethod(groups = "requires-bloomberg")
    public void beforeMethod() throws Exception{
        LOG.trace("beforeMethod");
        latch = new CountDownLatch(1);
        session = new DefaultBloombergSession();
        session.start();
    }

    @AfterMethod(groups = "requires-bloomberg")
    public void afterMethod() {
        LOG.trace("afterMethod - entry");
        session.stop();
        LOG.trace("afterMethod - exit");
    }

    @Test(groups = "requires-bloomberg")
    public void testEmptyList() throws Exception {
        LOG.trace("testEmptyList");
        assertFalse(latch.await(TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test(groups = "requires-bloomberg")
    public void testFeed() throws Exception {
        LOG.trace("testFeed");
        DataChangeListener lst = getDataChangeListener(LAST_PRICE, ASK, BID_SIZE);
        SubscriptionBuilder builder = new SubscriptionBuilder()
                .addSecurity("VGA Index")
                .addSecurity("EUR Curncy")
                .addField(LAST_PRICE)
                .addField(BID_SIZE)
                .addField(ASK)
                .addListener(lst);
        session.subscribe(builder);
        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS));
    }

    /**
     * in this test, the feed is started first and securities and fields are added afterwards
     */
    @Test(groups = "requires-bloomberg")
    public void testFeedNewAdditions() throws Exception {
        LOG.trace("testFeedNewAdditions");
        DataChangeListener lst = getDataChangeListener("GBP Curncy");
        SubscriptionBuilder builder = new SubscriptionBuilder()
                .addSecurity("VGA Index")
                .addSecurity("EUR Curncy")
                .addField(LAST_PRICE)
                .addField(BID_SIZE)
                .addField(ASK)
                .addListener(lst);

        session.subscribe(builder);
        assertFalse(latch.await(50, TimeUnit.MILLISECONDS)); //only works if the GBP has been caught

        builder = new SubscriptionBuilder()
                .addSecurity("GBP Curncy")
                .addField(ASK)
                .addListener(lst);

        session.subscribe(builder);
        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS)); //only works if the GBP has been caught
    }

    /**
     * Wrong ticker is ignored
     */
    @Test(groups = "requires-bloomberg")
    public void testWrongTicker() throws Exception {
        LOG.trace("testWrongTicker");
        DataChangeListener lst = getDataChangeListener();
        SubscriptionBuilder builder = new SubscriptionBuilder()
                .addSecurity("WHAT TICKER IS THAT")
                .addSecurity("EUR Curncy")
                .addField(LAST_PRICE)
                .addListener(lst);
        session.subscribe(builder);
        assertTrue(latch.await(TIMEOUT, TimeUnit.SECONDS));
    }

    private DataChangeListener getDataChangeListener() {
        return (e) -> latch.countDown();
    }
    private DataChangeListener getDataChangeListener(String... tickers) {
        final Set<String> tickerSet = new HashSet<> (Arrays.asList(tickers));
        return (e) -> {
            if (tickerSet.contains(e.getSource())) {
                latch.countDown();
            }
        };
    }
    private DataChangeListener getDataChangeListener(final RealtimeField... fields) {
        final Set<RealtimeField> fieldSet = new HashSet<> (Arrays.asList(fields));
        return (e) -> {
            if (fieldSet.contains(RealtimeField.valueOf(e.getDataName()))) {
                latch.countDown();
            }
        };
    }
}