package com.intellishare.casefile;
import com.intellishare.model.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
@Entity @Table(name="case_evidence")
public class CaseEvidence {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="case_id",nullable=false) private InvestigationCase caseRecord;
    @Column(nullable=false,length=255) private String title;
    @Column(columnDefinition="text") private String description;
    @Column(name="file_url",length=500) private String fileUrl;
    @Column(name="file_type",length=100) private String fileType;
    @Column(name="file_size_bytes") private Long fileSizeBytes;
    @Column(name="sha256_hash",length=64) private String sha256Hash;
    @Column(name="blockchain_tx_id",length=100) private String blockchainTxId;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="uploaded_by_id") private User uploadedBy;
    @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
    protected CaseEvidence(){}
    public CaseEvidence(String title,String description,String fileUrl,String fileType,Long size,String hash,String tx,User user){
        this.title=title;this.description=description;this.fileUrl=fileUrl;this.fileType=fileType;fileSizeBytes=size;sha256Hash=hash;blockchainTxId=tx;uploadedBy=user;
    }
    @PrePersist void onCreate(){createdAt=OffsetDateTime.now();}
    void setCaseRecord(InvestigationCase value){caseRecord=value;}
    public Long getId(){return id;} public String getTitle(){return title;} public String getDescription(){return description;}
    public String getFileUrl(){return fileUrl;} public String getFileType(){return fileType;} public Long getFileSizeBytes(){return fileSizeBytes;}
    public String getSha256Hash(){return sha256Hash;} public String getBlockchainTxId(){return blockchainTxId;}
    public User getUploadedBy(){return uploadedBy;} public OffsetDateTime getCreatedAt(){return createdAt;}
}
