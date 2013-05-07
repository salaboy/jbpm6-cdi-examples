package com.saiful.phdprocess;
/*
 * Copyright 2013 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

//import org.drools.event.rule.ActivationCreatedEvent;
import org.drools.core.event.ActivationCreatedEvent;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;
import org.jbpm.runtime.manager.impl.cdi.InjectableRegisterableItemsFactory;
import org.jbpm.runtime.manager.impl.task.SynchronizedTaskService;
import org.jbpm.runtime.manager.util.TestUtil;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.event.process.DefaultProcessEventListener;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.RuleFlowGroupActivatedEvent;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.Context;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.manager.RuntimeManagerFactory;
import org.kie.internal.runtime.manager.context.EmptyContext;


import bitronix.tm.resource.jdbc.PoolingDataSource;

/**
 *
 * @author salaboy/saiful
 */
@RunWith(Arquillian.class)
public class RulesIntegrationTest {

	 @Deployment()
	    public static Archive<?> createDeployment() {
	        return ShrinkWrap.create(JavaArchive.class, "hiring-example.jar")
	                .addPackage("org.jboss.seam.persistence") //seam-persistence
	                .addPackage("org.jboss.seam.transaction") //seam-persistence
	                .addPackage("org.jbpm.services.task")
	                .addPackage("org.jbpm.services.task.wih") // work items org.jbpm.services.task.wih
	                .addPackage("org.jbpm.services.task.annotations")
	                .addPackage("org.jbpm.services.task.api")
	                .addPackage("org.jbpm.services.task.impl")
	                .addPackage("org.jbpm.services.task.events")
	                .addPackage("org.jbpm.services.task.exception")
	                .addPackage("org.jbpm.services.task.identity")
	                .addPackage("org.jbpm.services.task.factories")
	                .addPackage("org.jbpm.services.task.internals")
	                .addPackage("org.jbpm.services.task.internals.lifecycle")
	                .addPackage("org.jbpm.services.task.lifecycle.listeners")
	                .addPackage("org.jbpm.services.task.query")
	                .addPackage("org.jbpm.services.task.util")
	                .addPackage("org.jbpm.services.task.commands") // This should not be required here
	                .addPackage("org.jbpm.services.task.deadlines") // deadlines
	                .addPackage("org.jbpm.services.task.deadlines.notifications.impl")
	                .addPackage("org.jbpm.services.task.subtask")
	                .addPackage("org.kie.internal.runtime")
	                .addPackage("org.kie.internal.runtime.manager")
	                .addPackage("org.kie.internal.runtime.manager.cdi.qualifier")
	                .addPackage("org.jbpm.runtime.manager")
	                .addPackage("org.jbpm.runtime.manager.impl")
	                .addPackage("org.jbpm.runtime.manager.impl.cdi")
	                .addPackage("org.jbpm.runtime.manager.impl.cdi.qualifier")
	                .addPackage("org.jbpm.runtime.manager.impl.context")
	                .addPackage("org.jbpm.runtime.manager.impl.factory")
	                .addPackage("org.jbpm.runtime.manager.impl.jpa")
	                .addPackage("org.jbpm.runtime.manager.impl.manager")
	                .addPackage("org.jbpm.runtime.manager.mapper")
	                .addPackage("org.jbpm.runtime.manager.impl.task")
	                .addPackage("org.jbpm.runtime.manager.impl.tx")
	                .addPackage("org.jbpm.shared.services.api")
	                .addPackage("org.jbpm.shared.services.impl")
	                .addPackage("org.jbpm.kie.services.api")
	                .addPackage("org.jbpm.kie.services.impl")
	                .addPackage("org.jbpm.kie.services.api.bpmn2")
	                .addPackage("org.jbpm.kie.services.impl.bpmn2")
	                .addPackage("org.jbpm.kie.services.impl.event.listeners")
	                .addPackage("org.jbpm.kie.services.impl.audit")
	                .addPackage("org.jbpm.kie.services.impl.util")
	                .addPackage("org.jbpm.kie.services.impl.vfs")
	                .addPackage("org.jbpm.kie.services.impl.example")
	                .addPackage("org.kie.commons.java.nio.fs.jgit")
	                .addPackage("com.salaboy.hiring.process")
	                .addAsResource("jndi.properties", "jndi.properties")
	                .addAsManifestResource("META-INF/persistence.xml", ArchivePaths.create("persistence.xml"))
	                //                .addAsManifestResource("META-INF/Taskorm.xml", ArchivePaths.create("Taskorm.xml"))
	                .addAsManifestResource("META-INF/beans.xml", ArchivePaths.create("beans.xml"));

	    }

    private static PoolingDataSource pds;

    @BeforeClass
    public static void setup() {
        TestUtil.cleanupSingletonSessionId();
        pds = TestUtil.setupPoolingDataSource();


    }

    @AfterClass
    public static void teardown() {
        pds.close();
    }

    @After
    public void tearDownTest() {
    }
    @Inject
    private EntityManagerFactory emf;
    @Inject
    private BeanManager beanManager;
    @Inject
    private RuntimeManagerFactory managerFactory;

    @Test
    public void simpleExecutionTest() {
        assertNotNull(managerFactory);
        RuntimeEnvironmentBuilder builder = RuntimeEnvironmentBuilder.getDefault()
                .entityManagerFactory(emf)
                .registerableItemsFactory(InjectableRegisterableItemsFactory.getFactory(beanManager, null));

        builder.addAsset(ResourceFactory.newClassPathResource("phdrepo/mapping.drl"), ResourceType.DRL);
        builder.addAsset(ResourceFactory.newClassPathResource("phdrepo/BPMN2-RuleTask2.drl"), ResourceType.DRL);
        builder.addAsset(ResourceFactory.newClassPathResource("phdrepo/BPMN2-RuleTask2.bpmn2"), ResourceType.BPMN2);

        org.kie.api.runtime.manager.RuntimeManager manager = managerFactory.newSingletonRuntimeManager(builder.get());
        testHiringProcess(manager, EmptyContext.get());

        manager.close();

    }

    private void testHiringProcess(RuntimeManager manager, Context context) {

 
    	RuntimeEngine runtime = manager.getRuntimeEngine(context);
        final KieSession ksession = runtime.getKieSession();
 
        ksession.addEventListener(
                new DefaultAgendaEventListener() {
                    //@Override
                    public void activationCreated(ActivationCreatedEvent event) {
                        System.out.println("Firing All the Rules! " + event);
                        ksession.fireAllRules();
                    }

                    @Override
                    public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
                        System.out.println("Firing All the Rules! " + event);
                    	ksession.fireAllRules();
                    }
                });
        

        
        TaskService taskService = runtime.getTaskService();

        assertNotNull(runtime);
        assertNotNull(ksession);
        
//      List<String> list = new ArrayList<String>();
//		ksession.setGlobal("list", list);
		
		Student student = new Student("Saiful", 19);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("student", student);
        //params.put("studentid", "saiful");
        
        ProcessInstance processInstance = ksession.createProcessInstance("com.saiful.phdprocess.DynamicAdaptation", params);
        System.out.println("Variables: " + ((WorkflowProcessInstanceImpl) processInstance).getVariables());

        final FactHandle processHandle = ksession.insert(processInstance);

        ksession.addEventListener(new DefaultProcessEventListener() {
            @Override
            public void beforeProcessStarted(ProcessStartedEvent event) {
                System.out.println("Firing All the Rules! " + event);
                ksession.fireAllRules();
            }

            @Override
            public void afterProcessStarted(ProcessStartedEvent event) {
                System.out.println("Firing All the Rules! " + event);
            	ksession.fireAllRules();
            }

            @Override
            public void afterProcessCompleted(ProcessCompletedEvent event) {
                System.out.println("Firing All the Rules! " + event);
            	ksession.retract(processHandle);
            }
        });
        
        ksession.startProcessInstance(processInstance.getId());

        assertEquals(processInstance.getState(), ProcessInstance.STATE_PENDING);
        
		ksession.fireAllRules();

		List<TaskSummary> tasks = ((SynchronizedTaskService)taskService).getTasksAssignedByGroup("staff", "en-UK");
        TaskSummary readinessReview = tasks.get(0);
        
//        Task readinessReviewTask = taskService.getTaskById(readinessReview.getId());
//        Content contentById = taskService.getContentById(readinessReviewTask.getTaskData().getDocumentContentId());
//        assertNotNull(contentById);
//
//        Map<String, Object> taskContent = (Map<String, Object>) ContentMarshallerHelper.unmarshall(contentById.getContent(), null);
//        assertEquals("saiful", taskContent.get("in.studentid"));
        
        taskService.claim(readinessReview.getId(), "paul");
        taskService.start(readinessReview.getId(), "paul");

        Map<String, Object> hrOutput = new HashMap<String, Object>();
        hrOutput.put("out.readiness", "no");

        taskService.complete(readinessReview.getId(), "paul", hrOutput);
        //assertNotNull(contentById);
        

		//assertTrue(list.size() == 1);
        assertProcessInstanceCompleted(processInstance.getId(), ksession);   
        
        System.out.println(">>> Removed Test>");
    }
    
	private void assertProcessInstanceCompleted(long processInstanceId, KieSession ksession) {
		assertNull(ksession.getProcessInstance(processInstanceId));
	}
}
