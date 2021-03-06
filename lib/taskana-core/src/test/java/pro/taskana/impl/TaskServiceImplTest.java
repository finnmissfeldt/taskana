package pro.taskana.impl;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import pro.taskana.Attachment;
import pro.taskana.Classification;
import pro.taskana.ObjectReference;
import pro.taskana.Task;
import pro.taskana.TaskState;
import pro.taskana.TaskSummary;
import pro.taskana.Workbasket;
import pro.taskana.WorkbasketPermission;
import pro.taskana.WorkbasketService;
import pro.taskana.configuration.TaskanaEngineConfiguration;
import pro.taskana.exceptions.InvalidStateException;
import pro.taskana.exceptions.NotAuthorizedException;
import pro.taskana.exceptions.TaskNotFoundException;
import pro.taskana.exceptions.WorkbasketNotFoundException;
import pro.taskana.mappings.AttachmentMapper;
import pro.taskana.mappings.ObjectReferenceMapper;
import pro.taskana.mappings.TaskMapper;
import pro.taskana.security.CurrentUserContext;

/**
 * Unit Test for TaskServiceImpl.
 *
 * @author EH
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(CurrentUserContext.class)
@PowerMockIgnore("javax.management.*")
public class TaskServiceImplTest {

    @InjectMocks
    private TaskServiceImpl cut;

    @Mock
    private TaskanaEngineConfiguration taskanaEngineConfigurationMock;

    @Mock
    private TaskanaEngineImpl taskanaEngineMock;

    @Mock
    private TaskMapper taskMapperMock;

    @Mock
    private ObjectReferenceMapper objectReferenceMapperMock;

    @Mock
    private WorkbasketService workbasketServiceMock;

    @Mock
    private ClassificationServiceImpl classificationServiceImplMock;

    @Mock
    private AttachmentMapper attachmentMapperMock;

    @Mock
    private ClassificationQueryImpl classificationQueryImplMock;

    @Mock
    private WorkbasketQueryImpl workbasketQueryImplMock;

    @Mock
    private SqlSession sqlSessionMock;

    @Before
    public void setup() throws WorkbasketNotFoundException {
        MockitoAnnotations.initMocks(this);
        when(taskanaEngineMock.getWorkbasketService()).thenReturn(workbasketServiceMock);
        when(taskanaEngineMock.getClassificationService()).thenReturn(classificationServiceImplMock);
        try {
            Mockito.doNothing().when(workbasketServiceMock).checkAuthorization(any(), any());
        } catch (NotAuthorizedException e) {
            e.printStackTrace();
        }
        Mockito.doNothing().when(taskanaEngineMock).openConnection();
        Mockito.doNothing().when(taskanaEngineMock).returnConnection();
    }

    @Test
    public void testTransferTaskToDestinationWorkbasketWithoutSecurity()
        throws TaskNotFoundException, WorkbasketNotFoundException, NotAuthorizedException, InvalidStateException {
        TaskServiceImpl cutSpy = Mockito.spy(cut);
        Workbasket destinationWorkbasket = createWorkbasket("2", "k1");
        Workbasket sourceWorkbasket = createWorkbasket("47", "key47");
        Classification dummyClassification = createDummyClassification();
        TaskImpl task = createUnitTestTask("1", "Unit Test Task 1", "key47", dummyClassification);
        task.setWorkbasketSummary(sourceWorkbasket.asSummary());
        task.setRead(true);
        when(workbasketServiceMock.getWorkbasket(destinationWorkbasket.getId())).thenReturn(destinationWorkbasket);
        when(workbasketServiceMock.getWorkbasket(sourceWorkbasket.getId())).thenReturn(sourceWorkbasket);
        when(taskanaEngineMock.getConfiguration()).thenReturn(taskanaEngineConfigurationMock);
        when(taskanaEngineConfigurationMock.isSecurityEnabled()).thenReturn(false);
        doReturn(task).when(cutSpy).getTask(task.getId());
        doNothing().when(taskMapperMock).update(any());
        doNothing().when(workbasketServiceMock).checkAuthorization(destinationWorkbasket.getId(),
            WorkbasketPermission.APPEND);
        doNothing().when(workbasketServiceMock).checkAuthorization(sourceWorkbasket.getId(),
            WorkbasketPermission.TRANSFER);

        Task actualTask = cutSpy.transfer(task.getId(), destinationWorkbasket.getId());

        verify(taskanaEngineMock, times(1)).openConnection();
        verify(workbasketServiceMock, times(1)).checkAuthorization(destinationWorkbasket.getId(),
            WorkbasketPermission.APPEND);
        verify(workbasketServiceMock, times(1)).checkAuthorization(sourceWorkbasket.getId(),
            WorkbasketPermission.TRANSFER);
        verify(workbasketServiceMock, times(1)).getWorkbasket(destinationWorkbasket.getId());
        verify(taskMapperMock, times(1)).update(any());
        verify(taskanaEngineMock, times(1)).returnConnection();
        verifyNoMoreInteractions(attachmentMapperMock, taskanaEngineConfigurationMock, taskanaEngineMock,
            taskMapperMock, objectReferenceMapperMock, workbasketServiceMock, sqlSessionMock,
            classificationQueryImplMock);

        assertThat(actualTask.isRead(), equalTo(false));
        assertThat(actualTask.getState(), equalTo(TaskState.READY));
        assertThat(actualTask.isTransferred(), equalTo(true));
        assertThat(actualTask.getWorkbasketKey(), equalTo(destinationWorkbasket.getKey()));
    }

    @Test
    public void testTaskSummaryEqualsHashCode() throws InterruptedException {
        Classification classification = createDummyClassification();
        Workbasket wb = createWorkbasket("WB-ID", "WB-Key");
        Attachment attachment = JunitHelper.createDefaultAttachment();
        ObjectReference objectReference = JunitHelper.createDefaultObjRef();
        TaskImpl taskBefore = createUnitTestTask("ID", "taskName", wb.getKey(), classification);
        taskBefore.setPrimaryObjRef(objectReference);
        Thread.sleep(10);
        TaskImpl taskAfter = createUnitTestTask("ID", "taskName", wb.getKey(), classification);
        taskAfter.setPrimaryObjRef(objectReference);
        TaskSummary summaryBefore = taskBefore.asSummary();
        TaskSummary summaryAfter = taskAfter.asSummary();

        assertNotEquals(summaryBefore, summaryAfter);
        assertNotEquals(summaryBefore.hashCode(), summaryAfter.hashCode());

        taskAfter.setCreated(taskBefore.getCreated());
        taskAfter.setModified(taskBefore.getModified());
        summaryAfter = taskAfter.asSummary();
        assertEquals(summaryBefore, summaryAfter);
        assertEquals(summaryBefore.hashCode(), summaryAfter.hashCode());

        taskBefore.setModified(null);
        summaryBefore = taskBefore.asSummary();
        assertNotEquals(summaryBefore, summaryAfter);
        assertNotEquals(summaryBefore.hashCode(), summaryAfter.hashCode());

        taskAfter.setModified(null);
        summaryAfter = taskAfter.asSummary();
        assertEquals(summaryBefore, summaryAfter);
        assertEquals(summaryBefore.hashCode(), summaryAfter.hashCode());
    }

    private TaskImpl createUnitTestTask(String id, String name, String workbasketKey, Classification classification) {
        TaskImpl task = new TaskImpl();
        task.setId(id);
        task.setExternalId(id);
        task.setName(name);
        task.setWorkbasketKey(workbasketKey);
        task.setDomain("");
        task.setAttachments(new ArrayList<>());
        Instant now = Instant.now().minus(Duration.ofMinutes(1L));
        task.setCreated(now);
        task.setModified(now);
        if (classification == null) {
            classification = createDummyClassification();
        }
        task.setClassificationSummary(classification.asSummary());
        task.setClassificationKey(classification.getKey());
        task.setDomain(classification.getDomain());
        return task;
    }

    private Classification createDummyClassification() {
        ClassificationImpl classification = new ClassificationImpl();
        classification.setName("dummy-classification");
        classification.setDomain("dummy-domain");
        classification.setKey("dummy-classification-key");
        classification.setId("DummyClassificationId");
        return classification;
    }

    private WorkbasketImpl createWorkbasket(String id, String key) {
        WorkbasketImpl workbasket = new WorkbasketImpl();
        workbasket.setId(id);
        workbasket.setDomain("Domain1");
        workbasket.setKey(key);
        workbasket.setName("Workbasket " + id);
        return workbasket;
    }
}
