package com.tracker.util;

import java.util.Map;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;

public class TaskUtil {

    public static String queueTask(String url) {
        return queueTask(url, null, null, null);
    }

    public static String queueTask(String url, Map<String, String> params) {
        return queueTask(url, null, null, params);
    }
    
    public static String queueTask(String url, String queueName, Map<String, String> params) {
        return queueTask(url, queueName, null, params);
    }

    public static String queueTask(String url, String queueName, Long etaMillis, Map<String, String> params) {
        TaskOptions taskOptions = TaskOptions.Builder
                .withMethod(TaskOptions.Method.GET)
                .url(url)
                .etaMillis(System.currentTimeMillis())
                .retryOptions(RetryOptions.Builder.withTaskRetryLimit(0));

        if(params != null) {
            for(Map.Entry<String, String> entry : params.entrySet()) {
                taskOptions.param(entry.getKey(), entry.getValue());
            }
        }

        Queue queue;
        if(queueName == null) {
            queue = QueueFactory.getDefaultQueue();
        } else {
            queue = QueueFactory.getQueue(queueName);
        }
        TaskHandle taskHandle = queue.add(taskOptions);
        return String.format("Queued task %s", taskHandle.getName());
    }
}
