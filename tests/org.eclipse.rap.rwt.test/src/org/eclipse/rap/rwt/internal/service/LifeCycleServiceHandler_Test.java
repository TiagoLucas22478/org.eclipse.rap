/*******************************************************************************
 * Copyright (c) 2002, 2014 Innoopract Informationssysteme GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Innoopract Informationssysteme GmbH - initial API and implementation
 *    EclipseSource - ongoing development
 ******************************************************************************/
package org.eclipse.rap.rwt.internal.service;

import static org.eclipse.rap.rwt.internal.service.ContextProvider.getApplicationContext;
import static org.eclipse.rap.rwt.internal.service.ContextProvider.getServiceStore;
import static org.eclipse.rap.rwt.internal.service.ContextProvider.getUISession;
import static org.eclipse.rap.rwt.internal.service.LifeCycleServiceHandler.markSessionStarted;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.rap.json.JsonArray;
import org.eclipse.rap.json.JsonObject;
import org.eclipse.rap.rwt.client.Client;
import org.eclipse.rap.rwt.client.WebClient;
import org.eclipse.rap.rwt.internal.application.ApplicationContextImpl;
import org.eclipse.rap.rwt.internal.lifecycle.RequestCounter;
import org.eclipse.rap.rwt.internal.protocol.ClientMessage;
import org.eclipse.rap.rwt.internal.protocol.ClientMessageConst;
import org.eclipse.rap.rwt.internal.protocol.Message;
import org.eclipse.rap.rwt.internal.protocol.MessageImpl;
import org.eclipse.rap.rwt.internal.protocol.ProtocolUtil;
import org.eclipse.rap.rwt.service.ServiceHandler;
import org.eclipse.rap.rwt.service.UISession;
import org.eclipse.rap.rwt.service.UISessionEvent;
import org.eclipse.rap.rwt.service.UISessionListener;
import org.eclipse.rap.rwt.testfixture.Fixture;
import org.eclipse.rap.rwt.testfixture.TestRequest;
import org.eclipse.rap.rwt.testfixture.TestResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;


public class LifeCycleServiceHandler_Test {

  private static final String SESSION_STORE_ATTRIBUTE = "session-store-attribute";
  private static final String HTTP_SESSION_ATTRIBUTE = "http-session-attribute";

  private static final int THREAD_COUNT = 10;
  private static final String ENTER = "enter|";
  private static final String EXIT = "exit|";

  private RWTMessageHandler messageHandler;
  private StartupPage startupPage;
  private LifeCycleServiceHandler serviceHandler;
  private StringBuilder log;

  @Before
  public void setUp() {
    Fixture.setUp();
    messageHandler = mockMessageHandler();
    startupPage = mock( StartupPage.class );
    serviceHandler = new LifeCycleServiceHandler( messageHandler, startupPage );
    log = new StringBuilder();
  }

  @After
  public void tearDown() {
    Fixture.tearDown();
  }

  @Test
  public void testRequestSynchronization() throws InterruptedException {
    List<Thread> threads = new ArrayList<Thread>();
    // initialize session, see bug 344549
    getUISession();
    ServiceContext context = ContextProvider.getContext();
    for( int i = 0; i < THREAD_COUNT; i++ ) {
      ServiceHandler syncHandler = new TestHandler( messageHandler, startupPage );
      Thread thread = new Thread( new Worker( context, syncHandler ) );
      thread.setDaemon( true );
      thread.start();
      threads.add( thread );
    }
    while( threads.size() > 0 ) {
      Thread thread = threads.get( 0 );
      thread.join();
      threads.remove( 0 );
    }
    String expected = "";
    for( int i = 0; i < THREAD_COUNT; i++ ) {
      expected += ENTER + EXIT;
    }
    assertEquals( expected, log.toString() );
  }

  @Test
  public void testUISessionClearedOnSessionRestart() throws IOException {
    UISession uiSession = getUISession();
    uiSession.setAttribute( SESSION_STORE_ATTRIBUTE, new Object() );

    markSessionStarted();
    simulateInitialUiRequest();
    service( serviceHandler );

    assertNull( uiSession.getAttribute( SESSION_STORE_ATTRIBUTE ) );
  }

  @Test
  public void testShutdownUISession() throws IOException {
    UISession uiSession = getUISession();

    markSessionStarted();
    simulateShutdownUiRequest();
    service( serviceHandler );

    assertFalse( uiSession.isBound() );
  }

  @Test
  public void testShutdownUISession_returnsValidJson() throws IOException {
    markSessionStarted();
    simulateShutdownUiRequest();
    service( serviceHandler );

    TestResponse response = getResponse();
    JsonObject message = JsonObject.readFrom( response.getContent() );
    assertEquals( "application/json; charset=UTF-8", response.getHeader( "Content-Type" ) );
    assertNotNull( message.get( "head" ) );
    assertNotNull( message.get( "operations" ) );
  }

  @Test
  public void testShutdownUISession_removesUISessionFromHttpSession() throws IOException {
    UISession uiSession = getUISession();
    HttpSession httpSession = uiSession.getHttpSession();

    markSessionStarted();
    simulateShutdownUiRequest();
    service( serviceHandler );

    assertNull( UISessionImpl.getInstanceFromSession( httpSession, null ) );
  }

  @Test
  public void testStartUISession_AfterPreviousShutdown() throws IOException {
    UISession oldUiSession = getUISession();

    markSessionStarted();
    simulateShutdownUiRequest();
    service( serviceHandler );

    simulateInitialUiRequest();
    service( serviceHandler );

    UISession newUiSession = getUISession();
    assertNotSame( oldUiSession, newUiSession );
  }

  @Test
  public void testUISessionListerenerCalledOnce_AfterPreviousShutdown() throws IOException {
    UISession uiSession = getUISession();
    UISessionListener listener = mock( UISessionListener.class );
    uiSession.addUISessionListener(listener );

    markSessionStarted();
    simulateShutdownUiRequest();
    service( serviceHandler );

    simulateInitialUiRequest();
    service( serviceHandler );

    verify( listener, times( 1 ) ).beforeDestroy( any( UISessionEvent.class ) );
  }

  @Test
  public void testHttpSessionNotClearedOnSessionRestart() throws IOException {
    HttpSession httpSession = getUISession().getHttpSession();
    Object attribute = new Object();
    httpSession.setAttribute( HTTP_SESSION_ATTRIBUTE, attribute );

    markSessionStarted();
    simulateInitialUiRequest();
    service( serviceHandler );

    assertSame( attribute, httpSession.getAttribute( HTTP_SESSION_ATTRIBUTE ) );
  }

  @Test
  public void testApplicationContextAfterSessionRestart() throws IOException {
    markSessionStarted();
    simulateInitialUiRequest();
    ApplicationContextImpl applicationContext = getApplicationContext();

    service( serviceHandler );

    UISession uiSession = getUISession();
    assertSame( applicationContext, uiSession.getApplicationContext() );
  }

  /*
   * When cleaning the session store, the display is disposed. This put a list with all disposed
   * widgets into the service store. As application is restarted in the same request, we have to
   * prevent these dispose calls to be rendered.
   * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=373084
   */
  @Test
  public void testClearServiceStoreAfterSessionRestart() throws IOException {
    markSessionStarted();
    simulateInitialUiRequest();
    service( serviceHandler );

    simulateInitialUiRequest();
    getServiceStore().setAttribute( "foo", "bar" );
    service( serviceHandler );

    assertNull( getServiceStore().getAttribute( "foo" ) );
  }

  @Test
  public void testClearServiceStoreAfterSessionRestart_restoresMessage() throws IOException {
    markSessionStarted();
    simulateInitialUiRequest();
    service( serviceHandler );

    simulateInitialUiRequest();
    ClientMessage message = ProtocolUtil.getClientMessage();
    service( serviceHandler );

    assertEquals( message.toString(), ProtocolUtil.getClientMessage().toString() );
  }

  @Test
  public void testFinishesProtocolWriter() throws IOException {
    simulateUiRequest();

    service( serviceHandler );

    assertTrue( getResponse().getContent().contains( "\"head\":" ) );
  }

  @Test
  public void testContentType() throws IOException {
    markSessionStarted();
    simulateUiRequest();

    service( serviceHandler );

    assertEquals( "application/json; charset=UTF-8", getResponse().getHeader( "Content-Type" ) );
  }

  @Test
  public void testContentType_forSessionTimeout() throws IOException {
    simulateUiRequest();

    service( serviceHandler );

    assertEquals( "application/json; charset=UTF-8", getResponse().getHeader( "Content-Type" ) );
  }

  @Test
  public void testContentType_forIllegalRequestCounter() throws IOException {
    simulateUiRequestWithIllegalCounter();

    service( serviceHandler );

    assertEquals( "application/json; charset=UTF-8", getResponse().getHeader( "Content-Type" ) );
  }

  @Test
  public void testStartupJson_forOtherClients() throws IOException {
    Fixture.fakeNewGetRequest();
    Fixture.fakeClient( mock( Client.class ) );

    service( serviceHandler );

    verifyZeroInteractions( startupPage );
    assertEquals( "application/json; charset=UTF-8", getResponse().getHeader( "Content-Type" ) );
  }

  @Test
  public void testStartupPage_forWebClient() throws IOException {
    Fixture.fakeNewGetRequest();
    Fixture.fakeClient( mock( WebClient.class ) );

    service( serviceHandler );

    verify( startupPage ).send( getResponse() );
  }

  @Test
  public void testStartupPage_forHeadRequest() throws IOException {
    Fixture.fakeNewGetRequest();
    getRequest().setMethod( "HEAD" );
    Fixture.fakeClient( mock( WebClient.class ) );

    service( serviceHandler );

    verify( startupPage ).send( getResponse() );
  }

  @Test
  public void testStartupRequest_shutsDownUISession_ifExceptionInStartupPage() throws IOException {
    Fixture.fakeNewGetRequest();
    Fixture.fakeClient( mock( WebClient.class ) );
    StartupPage startupPage = mock( StartupPage.class );
    doThrow( new RuntimeException() ).when( startupPage ).send( any( HttpServletResponse.class ) );

    try {
      service( new LifeCycleServiceHandler( messageHandler, startupPage ) );
    } catch( RuntimeException exception ) {
    }

    assertNull( getUISession() );
  }

  @Test
  public void testHandleInvalidRequestCounter() throws IOException {
    markSessionStarted();
    simulateUiRequestWithIllegalCounter();

    service( serviceHandler );

    assertEquals( HttpServletResponse.SC_PRECONDITION_FAILED, getResponse().getStatus() );
    Message message = getMessageFromResponse();
    assertEquals( "invalid request counter", getError( message ) );
    assertTrue( message.getOperations().isEmpty() );
  }

  @Test
  public void testHandlesSessionTimeout() throws IOException {
    simulateUiRequest();

    service( serviceHandler );

    TestResponse response = getResponse();
    assertEquals( HttpServletResponse.SC_FORBIDDEN, response.getStatus() );
    Message message = getMessageFromResponse();
    assertEquals( "session timeout", getError( message ) );
    assertTrue( message.getOperations().isEmpty() );
  }

  @Test
  public void testHandlesInvalidRequestContentType() throws IOException {
    // SECURITY: Checking the content-type prevents CSRF attacks, see bug 413668
    // Also allows application to be started with POST request, see bug 416445
    simulateUiRequestWithIllegalContentType();

    service( serviceHandler );

    verify( startupPage ).send( any( HttpServletResponse.class ) );
  }

  @Test
  public void testSendBufferedResponse() throws IOException {
    markSessionStarted();
    simulateUiRequest();
    RequestCounter.getInstance().nextRequestId();
    int requestCounter = RequestCounter.getInstance().nextRequestId();
    Fixture.fakeHeadParameter( "requestCounter", requestCounter );

    service( serviceHandler );
    Message firstResponse = getMessageFromResponse();

    simulateUiRequest();
    Fixture.fakeHeadParameter( "requestCounter", requestCounter );
    service( serviceHandler );
    Message secondResponse = getMessageFromResponse();

    assertEquals( firstResponse.toString(), secondResponse.toString() );
  }

  @Test
  public void testHasValidRequestCounter_trueWithValidParameter() {
    int nextRequestId = RequestCounter.getInstance().nextRequestId();
    Message message = new MessageImpl();
    message.getHead().set( "requestCounter", nextRequestId );

    boolean valid = LifeCycleServiceHandler.hasValidRequestCounter( message );

    assertTrue( valid );
  }

  @Test
  public void testHasValidRequestCounter_falseWithInvalidParameter() {
    RequestCounter.getInstance().nextRequestId();
    Message message = new MessageImpl();
    message.getHead().set( "requestCounter", 23 );

    boolean valid = LifeCycleServiceHandler.hasValidRequestCounter( message );

    assertFalse( valid );
  }

  @Test
  public void testHasValidRequestCounter_failsWithIllegalParameterFormat() {
    RequestCounter.getInstance().nextRequestId();
    Message message = new MessageImpl();
    message.getHead().set( "requestCounter", "not-a-number" );

    try {
      LifeCycleServiceHandler.hasValidRequestCounter( message );
      fail();
    } catch( Exception exception ) {
      assertTrue( exception.getMessage().contains( "Not a number" ) );
    }
  }

  @Test
  public void testHasValidRequestCounter_toleratesMissingParameterInFirstRequest() {
    Message message = new MessageImpl();

    boolean valid = LifeCycleServiceHandler.hasValidRequestCounter( message );

    assertTrue( valid );
  }

  @Test
  public void testHasValidRequestCounter_falseWithMissingParameter() {
    RequestCounter.getInstance().nextRequestId();
    Message message = new MessageImpl();

    boolean valid = LifeCycleServiceHandler.hasValidRequestCounter( message );

    assertFalse( valid );
  }

  @Test
  public void testProcessesMessage() throws IOException {
    markSessionStarted();
    simulateUiRequest();
    JsonObject message = createExampleMessage();
    getRequest().setBody( message.toString() );
    ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass( Message.class );

    service( serviceHandler );

    verify( messageHandler ).handleMessage( messageCaptor.capture() );
    assertEquals( message, JsonObject.readFrom( messageCaptor.getValue().toString() ) );
  }

  @Test
  public void testUIRequest_shutsDownUISession_ifRuntimeExceptionInHandler() throws IOException {
    markSessionStarted();
    simulateUiRequest();
    RWTMessageHandler messageHandler = mock( RWTMessageHandler.class );
    doThrow( new RuntimeException() ).when( messageHandler ).handleMessage( any( Message.class ) );

    try {
      service( new LifeCycleServiceHandler( messageHandler, startupPage ) );
    } catch( RuntimeException exception ) {
    }

    assertNull( getUISession() );
  }

  @Test
  public void testUIRequest_shutsDownUISession_ifIOException() throws IOException {
    markSessionStarted();
    simulateUiRequest();
    HttpServletResponse response = mock( HttpServletResponse.class );
    doThrow( new IOException() ).when( response ).getWriter();

    try {
      serviceHandler.service( getRequest(), response );
    } catch( IOException exception ) {
    }

    assertNull( getUISession() );
  }

  private void simulateUiRequest() {
    Fixture.fakeNewRequest();
  }

  private void simulateInitialUiRequest() {
    Fixture.fakeNewRequest();
    Fixture.fakeHeadParameter( ClientMessageConst.RWT_INITIALIZE, true );
  }

  private void simulateShutdownUiRequest() {
    Fixture.fakeNewRequest();
    Fixture.fakeHeadParameter( ClientMessageConst.SHUTDOWN, true );
  }

  private void simulateUiRequestWithIllegalCounter() {
    Fixture.fakeNewRequest();
    Fixture.fakeHeadParameter( "requestCounter", 23 );
  }

  private void simulateUiRequestWithIllegalContentType() {
    Fixture.fakeNewRequest();
    getRequest().setContentType( "text/plain" );
  }

  private static RWTMessageHandler mockMessageHandler() {
    RWTMessageHandler messageHandler = mock( RWTMessageHandler.class );
    JsonObject message = createExampleMessage();
    when( messageHandler.handleMessage( any( Message.class ) ) ).thenReturn( message  );
    return messageHandler;
  }

  private static JsonObject createExampleMessage() {
    return new JsonObject()
      .add( "head", new JsonObject().add( "test", true ) )
      .add( "operations", new JsonArray() );
  }

  private static void service( LifeCycleServiceHandler serviceHandler ) throws IOException {
    serviceHandler.service( getRequest(), getResponse() );
  }

  private static TestRequest getRequest() {
    return ( TestRequest )ContextProvider.getRequest();
  }

  private static TestResponse getResponse() {
    return ( TestResponse )ContextProvider.getResponse();
  }

  private static Message getMessageFromResponse() {
    return new ClientMessage( JsonObject.readFrom( getResponse().getContent() ) );
  }

  private static String getError( Message message ) {
    return message.getHead().get( "error" ).asString();
  }

  private class TestHandler extends LifeCycleServiceHandler {

    public TestHandler( RWTMessageHandler messageHandler, StartupPage startupPage ) {
      super( messageHandler, startupPage );
    }

    @Override
    void synchronizedService( HttpServletRequest request, HttpServletResponse response ) {
      log.append( ENTER );
      try {
        Thread.sleep( 2 );
      } catch( InterruptedException e ) {
        // ignore
      }
      log.append( EXIT );
    }
  }

  private static class Worker implements Runnable {
    private final ServiceContext context;
    private final ServiceHandler serviceHandler;

    private Worker( ServiceContext context, ServiceHandler serviceHandler ) {
      this.context = context;
      this.serviceHandler = serviceHandler;
    }

    public void run() {
      ContextProvider.setContext( context );
      try {
        serviceHandler.service( context.getRequest(), context.getResponse() );
      } catch( Exception exception ) {
        throw new RuntimeException( exception );
      } finally {
        ContextProvider.releaseContextHolder();
      }
    }
  }

}
