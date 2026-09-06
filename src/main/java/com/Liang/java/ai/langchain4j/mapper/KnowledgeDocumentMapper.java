package com.Liang.java.ai.langchain4j.mapper;

import com.Liang.java.ai.langchain4j.entity.KnowledgeDocument;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {
    @Select("SELECT * FROM knowledge_document WHERE content_sha256 = #{sha256} LIMIT 1")
    KnowledgeDocument findByContentSha256(@Param("sha256") String sha256);

    @Select("SELECT COALESCE(MAX(version_no), 0) + 1 FROM knowledge_document WHERE document_key = #{documentKey}")
    int nextVersion(@Param("documentKey") String documentKey);

    @Select("SELECT * FROM knowledge_document WHERE document_key = #{documentKey} AND status = 'PUBLISHED' LIMIT 1")
    KnowledgeDocument findPublishedByDocumentKey(@Param("documentKey") String documentKey);

    @Select("SELECT * FROM knowledge_document ORDER BY document_key ASC, version_no DESC")
    List<KnowledgeDocument> findAllVersions();

    @Select("SELECT * FROM knowledge_document WHERE status = 'PUBLISHED' ORDER BY document_key ASC, version_no ASC")
    List<KnowledgeDocument> findAllPublished();

    @Update("UPDATE knowledge_document SET status = 'ARCHIVED', published_at = NULL "
            + "WHERE document_key = #{documentKey} AND status = 'PUBLISHED'")
    int archivePublished(@Param("documentKey") String documentKey);

    @Update("UPDATE knowledge_document SET status = 'PUBLISHED', published_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND status <> 'PUBLISHED'")
    int markPublished(@Param("id") Long id);
}
