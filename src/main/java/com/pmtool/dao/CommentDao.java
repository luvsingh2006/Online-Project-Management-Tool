package com.pmtool.dao;

import com.pmtool.model.Comment;
import java.util.List;

public interface CommentDao extends GenericDao<Comment, Long> {
    List<Comment> findByTaskId(Long taskId);
}
