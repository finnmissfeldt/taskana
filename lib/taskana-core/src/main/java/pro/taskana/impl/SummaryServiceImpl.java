package pro.taskana.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.taskana.SummaryService;
import pro.taskana.TaskanaEngine;
import pro.taskana.model.TaskSummary;
import pro.taskana.model.mappings.SummaryMapper;

/**
 * @author mle
 * Organization of Table-Summaries with less informations.
 */
public class SummaryServiceImpl implements SummaryService {

    public static final Logger LOGGER = LoggerFactory.getLogger(SummaryServiceImpl.class);
    private SummaryMapper summaryMapper;
    private TaskanaEngineImpl taskanaEngineImpl;

    public SummaryServiceImpl(TaskanaEngine taskanaEngine, SummaryMapper summaryMapper) {
        this.summaryMapper = summaryMapper;
        this.taskanaEngineImpl = (TaskanaEngineImpl) taskanaEngine;
    }

    @Override
    public List<TaskSummary> getTaskSummariesByWorkbasketId(String workbasketId) {
        List<TaskSummary> taskSummaries = new ArrayList<>();
        try {
            taskanaEngineImpl.openConnection();
            taskSummaries = summaryMapper.findTasksummariesByWorkbasketId(workbasketId);
        } catch (Exception ex) {
            LOGGER.error("Getting TASKSUMMARY failed internal.", ex);
        } finally {
            if (taskSummaries == null) {
                taskSummaries = new ArrayList<>();
            }
            taskanaEngineImpl.returnConnection();
        }
        return taskSummaries;
    }
}