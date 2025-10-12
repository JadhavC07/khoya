package com.project.khoya.service;

import com.project.khoya.dto.CommentResponse;
import com.project.khoya.entity.Comment;
import com.project.khoya.entity.CommentStatus;
import com.project.khoya.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CacheService {

    @Autowired
    private CommentRepository commentRepository;

    @Cacheable(value = "alertComments", key = "#alertId")
    public List<CommentResponse> getCachedComments(Long alertId) {
        List<Comment> rootComments = commentRepository.findByAlertIdAndParentIsNull(alertId);

        return rootComments.stream().filter(comment -> comment.getStatus() == CommentStatus.ACTIVE).map(this::mapToDto).toList();
    }


    @CacheEvict(value = "alertComments", key = "#alertId")
    public void evictCommentCache(Long alertId) {
        // Evict when new comments are added
    }

    private CommentResponse mapToDto(Comment comment) {

        CommentResponse dto = new CommentResponse();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());


        if (comment.getAuthor() != null) {
            CommentResponse.UserInfo authorInfo = new CommentResponse.UserInfo(comment.getAuthor().getId(), comment.getAuthor().getName(), comment.getAuthor().getEmail());
            dto.setAuthor(authorInfo);
        }

        dto.setUpvotes(comment.getUpvotes());
        dto.setDownvotes(comment.getDownvotes());
        dto.setScore(comment.getScore());
        dto.setCreatedAt(comment.getCreatedAt());


        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            List<CommentResponse> replyDtos = comment.getReplies().stream().filter(reply -> reply.getStatus() == CommentStatus.ACTIVE).map(this::mapToDto).toList();
            dto.setReplies(replyDtos);
        }

        return dto;
    }


}