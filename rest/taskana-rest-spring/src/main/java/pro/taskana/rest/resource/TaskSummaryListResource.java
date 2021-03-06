package pro.taskana.rest.resource;

import java.util.Collection;

import org.springframework.hateoas.Link;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resource class for {@link TaskSummaryResource} with Pagination.
 */
public class TaskSummaryListResource extends PagedResources<TaskSummaryResource> {

    public TaskSummaryListResource() {
        super();
    }

    public TaskSummaryListResource(Collection<TaskSummaryResource> content, PageMetadata metadata,
        Iterable<Link> links) {
        super(content, metadata, links);
    }

    public TaskSummaryListResource(Collection<TaskSummaryResource> content, PageMetadata metadata, Link... links) {
        super(content, metadata, links);
    }

    @Override
    @JsonProperty("tasks")
    public Collection<TaskSummaryResource> getContent() {
        return super.getContent();
    }

}
