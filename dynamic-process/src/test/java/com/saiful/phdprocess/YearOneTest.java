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

import static org.junit.Assert.assertNotNull;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;

import org.drools.core.spi.ProcessContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jbpm.process.instance.event.listeners.RuleAwareProcessEventLister;
import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;
import org.jbpm.runtime.manager.impl.cdi.InjectableRegisterableItemsFactory;
import org.jbpm.runtime.manager.impl.task.SynchronizedTaskService;
import org.jbpm.runtime.manager.util.TestUtil;
import org.jbpm.services.task.impl.factories.TaskFactory;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;

import org.kie.api.runtime.manager.Context;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.event.KnowledgeRuntimeEventManager;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.logger.KnowledgeRuntimeLoggerFactory;
import org.kie.internal.runtime.StatefulKnowledgeSession;
import org.kie.internal.runtime.manager.RuntimeManagerFactory;
import org.kie.internal.runtime.manager.context.EmptyContext;

import bitronix.tm.resource.jdbc.PoolingDataSource;

import static org.junit.Assert.*;


/**
 *
 * @author salaboy/saiful
 */
@RunWith(Arquillian.class)
public class YearOneTest {

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

        builder.addAsset(ResourceFactory.newClassPathResource("phdrepo/Rule-YearOne.drl"), ResourceType.DRL);
        builder.addAsset(ResourceFactory.newClassPathResource("phdrepo/YearOne.bpmn2"), ResourceType.BPMN2);

        org.kie.api.runtime.manager.RuntimeManager manager = managerFactory.newSingletonRuntimeManager(builder.get(), "dynamic-processes");

        testHiringProcess(manager, EmptyContext.get());

        manager.close();

    }

    private void testHiringProcess(RuntimeManager manager, Context context) {


        RuntimeEngine runtime = manager.getRuntimeEngine(context);
        final KieSession ksession = runtime.getKieSession();

		KnowledgeRuntimeLoggerFactory.newConsoleLogger((KnowledgeRuntimeEventManager) ksession);
        ksession.addEventListener(new RuleAwareProcessEventLister());

        TaskService taskService = runtime.getTaskService();

        assertNotNull(runtime);
        assertNotNull(ksession);

        ksession.setGlobal("taskService", taskService);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("studentNameSub","saiful");

        ProcessInstance processInstance = ksession.createProcessInstance("PGR.YearOne", params);
        //System.out.println("Variables: " + ((WorkflowProcessInstanceImpl) processInstance).getVariables());

        ksession.startProcessInstance(processInstance.getId());
        
        //Supervisory Meeting 1
        List<TaskSummary> task1 = ((SynchronizedTaskService) taskService).getTasksAssignedAsPotentialOwner("saiful", "en-UK");
        TaskSummary meeting1 = task1.get(0);
        System.out.println("saiful executing task " + meeting1.getName() + "(" + meeting1.getId() + ": " + meeting1.getDescription() + ")");
        taskService.start(meeting1.getId(), "saiful");
        Map<String, Object> meeting1Out = new HashMap<String, Object>();
        meeting1Out.put("SupervisoryMeetingOutput", "https://docs.google.com/document/d/1AiACaA1WOQRg3f-xjcB2iKjSPR6DkxG5Vp3fWccurDA/edit?usp=sharing");
        taskService.complete(meeting1.getId(), "saiful", meeting1Out);
        
        //Supervisory Meeting 2
        List<TaskSummary> task2 = ((SynchronizedTaskService) taskService).getTasksAssignedAsPotentialOwner("saiful", "en-UK");
        TaskSummary meeting2 = task2.get(0);
        System.out.println("saiful executing task " + meeting2.getName() + "(" + meeting2.getId() + ": " + meeting2.getDescription() + ")");
        taskService.start(meeting2.getId(), "saiful");
        Map<String, Object> meeting2Out = new HashMap<String, Object>();
        meeting2Out.put("SupervisoryMeetingOutput", "https://docs.google.com/document/d/1AiACaA1WOQRg3f-xjcB2iKjSPR6DkxG5Vp3fWccurDA/edit?usp=sharing");
        taskService.complete(meeting2.getId(), "saiful", meeting2Out);

        //Supervisory Meeting 3
        List<TaskSummary> task3 = ((SynchronizedTaskService) taskService).getTasksAssignedAsPotentialOwner("saiful", "en-UK");
        TaskSummary meeting3 = task3.get(0);
        System.out.println("saiful executing task " + meeting3.getName() + "(" + meeting3.getId() + ": " + meeting3.getDescription() + ")");
        taskService.start(meeting3.getId(), "saiful");
        Map<String, Object> meeting3Out = new HashMap<String, Object>();
        meeting2Out.put("SupervisoryMeetingOutput", "https://docs.google.com/document/d/1AiACaA1WOQRg3f-xjcB2iKjSPR6DkxG5Vp3fWccurDA/edit?usp=sharing");
        taskService.complete(meeting3.getId(), "saiful", meeting3Out);
              
        //Supervisory Meeting 4
        List<TaskSummary> task4 = ((SynchronizedTaskService) taskService).getTasksAssignedAsPotentialOwner("saiful", "en-UK");
        TaskSummary meeting4 = task4.get(0);
        System.out.println("saiful executing task " + meeting4.getName() + "(" + meeting4.getId() + ": " + meeting4.getDescription() + ")");
        taskService.start(meeting4.getId(), "saiful");
        Map<String, Object> meeting4Out = new HashMap<String, Object>();
        meeting4Out.put("SupervisoryMeetingOutput", "https://docs.google.com/document/d/1AiACaA1WOQRg3f-xjcB2iKjSPR6DkxG5Vp3fWccurDA/edit?usp=sharing");
        taskService.complete(meeting4.getId(), "saiful", meeting4Out);
        
        //Supervisory Meeting 5
        List<TaskSummary> task5 = ((SynchronizedTaskService) taskService).getTasksAssignedAsPotentialOwner("saiful", "en-UK");
        TaskSummary meeting5 = task5.get(0);
        System.out.println("saiful executing task " + meeting5.getName() + "(" + meeting5.getId() + ": " + meeting5.getDescription() + ")");
        taskService.start(meeting5.getId(), "saiful");
        Map<String, Object> meeting5Out = new HashMap<String, Object>();
        meeting5Out.put("SupervisoryMeetingOutput", "https://docs.google.com/document/d/1AiACaA1WOQRg3f-xjcB2iKjSPR6DkxG5Vp3fWccurDA/edit?usp=sharing");
        taskService.complete(meeting5.getId(), "saiful", meeting5Out);
        
        //Supervisory Meeting 6
        List<TaskSummary> task6 = ((SynchronizedTaskService) taskService).getTasksAssignedAsPotentialOwner("saiful", "en-UK");
        TaskSummary meeting6 = task6.get(0);
        System.out.println("saiful executing task " + meeting6.getName() + "(" + meeting6.getId() + ": " + meeting6.getDescription() + ")");
        taskService.start(meeting6.getId(), "saiful");
        Map<String, Object> meeting6Out = new HashMap<String, Object>();
        meeting6Out.put("SupervisoryMeetingOutput", "https://docs.google.com/document/d/1AiACaA1WOQRg3f-xjcB2iKjSPR6DkxG5Vp3fWccurDA/edit?usp=sharing");
        taskService.complete(meeting6.getId(), "saiful", meeting6Out);
        
        //Supervisory Meeting 7
        List<TaskSummary> task7 = ((SynchronizedTaskService) taskService).getTasksAssignedAsPotentialOwner("saiful", "en-UK");
        TaskSummary meeting7 = task7.get(0);
        System.out.println("saiful executing task " + meeting7.getName() + "(" + meeting7.getId() + ": " + meeting7.getDescription() + ")");
        taskService.start(meeting7.getId(), "saiful");
        Map<String, Object> meeting7Out = new HashMap<String, Object>();
        meeting7Out.put("SupervisoryMeetingOutput", "https://docs.google.com/document/d/1AiACaA1WOQRg3f-xjcB2iKjSPR6DkxG5Vp3fWccurDA/edit?usp=sharing");
        taskService.complete(meeting7.getId(), "saiful", meeting7Out);
        
        //Supervisory Meeting 8
        List<TaskSummary> task8 = ((SynchronizedTaskService) taskService).getTasksAssignedAsPotentialOwner("saiful", "en-UK");
        TaskSummary meeting8 = task8.get(0);
        System.out.println("saiful executing task " + meeting8.getName() + "(" + meeting8.getId() + ": " + meeting8.getDescription() + ")");
        taskService.start(meeting8.getId(), "saiful");
        Map<String, Object> meeting8Out = new HashMap<String, Object>();
        meeting8Out.put("SupervisoryMeetingOutput", "https://docs.google.com/document/d/1AiACaA1WOQRg3f-xjcB2iKjSPR6DkxG5Vp3fWccurDA/edit?usp=sharing");
        taskService.complete(meeting8.getId(), "saiful", meeting8Out);
        
        //Supervisory Meeting 9
        List<TaskSummary> task9 = ((SynchronizedTaskService) taskService).getTasksAssignedAsPotentialOwner("saiful", "en-UK");
        TaskSummary meeting9 = task9.get(0);
        System.out.println("saiful executing task " + meeting9.getName() + "(" + meeting9.getId() + ": " + meeting9.getDescription() + ")");
        taskService.start(meeting9.getId(), "saiful");
        Map<String, Object> meeting9Out = new HashMap<String, Object>();
        meeting9Out.put("SupervisoryMeetingOutput", "https://docs.google.com/document/d/1AiACaA1WOQRg3f-xjcB2iKjSPR6DkxG5Vp3fWccurDA/edit?usp=sharing");
        taskService.complete(meeting9.getId(), "saiful", meeting9Out);
        
        //Supervisory Meeting 10
        List<TaskSummary> task10 = ((SynchronizedTaskService) taskService).getTasksAssignedAsPotentialOwner("saiful", "en-UK");
        TaskSummary meeting10 = task10.get(0);
        System.out.println("saiful executing task " + meeting10.getName() + "(" + meeting10.getId() + ": " + meeting10.getDescription() + ")");
        taskService.start(meeting10.getId(), "saiful");
        Map<String, Object> meeting10Out = new HashMap<String, Object>();
        meeting10Out.put("SupervisoryMeetingOutput", "https://docs.google.com/document/d/1AiACaA1WOQRg3f-xjcB2iKjSPR6DkxG5Vp3fWccurDA/edit?usp=sharing");
        taskService.complete(meeting10.getId(), "saiful", meeting10Out);
        
        //Supervisory Meeting 11
        List<TaskSummary> task11 = ((SynchronizedTaskService) taskService).getTasksAssignedAsPotentialOwner("saiful", "en-UK");
        TaskSummary meeting11 = task11.get(0);
        System.out.println("saiful executing task " + meeting11.getName() + "(" + meeting11.getId() + ": " + meeting11.getDescription() + ")");
        taskService.start(meeting11.getId(), "saiful");
        Map<String, Object> meeting11Out = new HashMap<String, Object>();
        meeting11Out.put("SupervisoryMeetingOutput", "https://docs.google.com/document/d/1AiACaA1WOQRg3f-xjcB2iKjSPR6DkxG5Vp3fWccurDA/edit?usp=sharing");
        taskService.complete(meeting11.getId(), "saiful", meeting11Out);

        //Supervisory Meeting 12
        List<TaskSummary> task12 = ((SynchronizedTaskService) taskService).getTasksAssignedAsPotentialOwner("saiful", "en-UK");
        TaskSummary meeting12 = task12.get(0);
        System.out.println("saiful executing task " + meeting12.getName() + "(" + meeting12.getId() + ": " + meeting12.getDescription() + ")");
        taskService.start(meeting12.getId(), "saiful");
        Map<String, Object> meeting12Out = new HashMap<String, Object>();
        meeting12Out.put("SupervisoryMeetingOutput", "https://docs.google.com/document/d/1AiACaA1WOQRg3f-xjcB2iKjSPR6DkxG5Vp3fWccurDA/edit?usp=sharing");
        taskService.complete(meeting12.getId(), "saiful", meeting12Out);

        //Conduct Progress Review 
        List<TaskSummary> tasks13 = ((SynchronizedTaskService) taskService).getTasksAssignedByGroup("staff", "en-UK");
        TaskSummary progressReview1 = tasks13.get(0);
        System.out.println("paul executing task " + progressReview1.getName() + "(" + progressReview1.getId() + ": " + progressReview1.getDescription() + ")");
        taskService.claim(progressReview1.getId(), "paul");
        taskService.start(progressReview1.getId(), "paul");
        Map<String, Object> progressReview1Out = new HashMap<String, Object>();
        //progressReview1Out.put("ProgressReportOutput", "proceed");
        progressReview1Out.put("ProgressReportOutput", "resubmit");
        taskService.complete(progressReview1.getId(), "paul", progressReview1Out);
        ksession.fireAllRules();

        //Produce Annual Report 
        List<TaskSummary> tasks14 = ((SynchronizedTaskService) taskService).getTasksAssignedByGroup("staff", "en-UK");
        TaskSummary annualReport = tasks14.get(0);
        System.out.println("chris executing task " + annualReport.getName() + "(" + annualReport.getId() + ": " + annualReport.getDescription() + ")");
        taskService.claim(annualReport.getId(), "chris");
        taskService.start(annualReport.getId(), "chris");
        Map<String, Object> annualReportOut = new HashMap<String, Object>();
        annualReportOut.put("ProgressReportOutput", "https://docs.google.com/document/d/1HLyupmbYb_MYO5omBqZjjogdaRpozC9muIVmqN1U5fs/edit?usp=sharing");
        taskService.complete(annualReport.getId(), "chris", annualReportOut);
        
        //Reregister
        List<TaskSummary> tasks15 = ((SynchronizedTaskService) taskService).getTasksAssignedByGroup("admin", "en-UK");
        TaskSummary reRegister = tasks15.get(0);
        System.out.println("judith executing task " + reRegister.getName() + "(" + reRegister.getId() + ": " + reRegister.getDescription() + ")");
        taskService.claim(reRegister.getId(), "judith");
        taskService.start(reRegister.getId(), "judith");
        Map<String, Object> reregisterOut = new HashMap<String, Object>();
        reregisterOut.put("ProgressReportOutput", "yes");
        taskService.complete(reRegister.getId(), "judith", reregisterOut);
        
        assertProcessInstanceCompleted(processInstance.getId(), ksession);

        System.out.println("<<<<<<<< Process Complete >>>>>>>>");
        
    }
	private void assertProcessInstanceCompleted(long processInstanceId, KieSession ksession) {
		assertNull(ksession.getProcessInstance(processInstanceId));
	}

}