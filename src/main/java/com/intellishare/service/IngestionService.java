package com.intellishare.service;

import com.intellishare.audit.AuditService;
import com.intellishare.casefile.CaseTimeline;
import com.intellishare.casefile.InvestigationCase;
import com.intellishare.dto.ingestion.IngestionResponse;
import com.intellishare.model.User;
import com.intellishare.repository.InvestigationCaseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Service
public class IngestionService {
    private static final List<String> HEADER = List.of("caseNumber", "title", "type", "status", "priority", "incidentDate", "city", "address", "description");
    private final JdbcTemplate jdbc;
    private final InvestigationCaseRepository cases;
    private final AuditService audit;

    public IngestionService(JdbcTemplate jdbc, InvestigationCaseRepository cases, AuditService audit) { this.jdbc=jdbc; this.cases=cases; this.audit=audit; }

    @Transactional
    public IngestionResponse importCases(User actor, MultipartFile file) {
        if (!"Administrator".equals(actor.getRole()) && !"Officer".equals(actor.getRole())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Your role cannot import case records");
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a non-empty UTF-8 CSV file");
        String name=file.getOriginalFilename()==null?"cases.csv":file.getOriginalFilename();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".csv")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only .csv files are supported");
        if (file.getSize() > 2_000_000) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"CSV files must be 2 MB or smaller");
        List<List<String>> rows;
        try { rows=parse(new String(file.getBytes(),StandardCharsets.UTF_8)); }
        catch (IOException | IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"CSV could not be read: "+e.getMessage()); }
        if (rows.isEmpty() || !rows.getFirst().equals(HEADER)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"CSV header must be: "+String.join(",",HEADER));
        if (rows.size() < 2) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"CSV has no case rows");
        if (rows.size() > 501) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"CSV may contain at most 500 case rows");
        List<String> errors=new ArrayList<>(); Set<String> seen=new HashSet<>();
        for(int i=1;i<rows.size();i++) {
            List<String> row=rows.get(i); int line=i+1;
            if(row.size()!=HEADER.size()) { errors.add("Row "+line+": expected "+HEADER.size()+" columns"); continue; }
            String number=row.get(0).trim();
            if(number.isBlank() || !number.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,49}")) errors.add("Row "+line+": invalid caseNumber");
            if(row.get(1).isBlank() || row.get(1).length()>255) errors.add("Row "+line+": title is required and limited to 255 characters");
            if(row.get(2).isBlank() || row.get(2).length()>100) errors.add("Row "+line+": type is required and limited to 100 characters");
            if(!Set.of("Registered","Under Investigation","Closed","Cold Case").contains(row.get(3))) errors.add("Row "+line+": invalid status");
            if(!Set.of("High","Medium","Low").contains(row.get(4))) errors.add("Row "+line+": invalid priority");
            if(!row.get(5).isBlank()) try { LocalDate.parse(row.get(5)); } catch(Exception ex) { errors.add("Row "+line+": incidentDate must use YYYY-MM-DD"); }
            if(row.get(6).length()>100 || row.get(7).length()>255 || row.get(8).length()>20000) errors.add("Row "+line+": city, address, or description exceeds its limit");
            if(!seen.add(number.toLowerCase(Locale.ROOT)) || cases.existsByCaseNumberIgnoreCase(number)) errors.add("Row "+line+": caseNumber already exists or is duplicated in this file");
        }
        UUID jobId=UUID.randomUUID();
        if(!errors.isEmpty()) {
            persistJob(jobId,name,"FAILED",0,actor);
            audit.record(actor,"INGESTION_FAILED","ingestion:"+jobId,"CSV validation failed with "+errors.size()+" row errors");
            return new IngestionResponse(jobId,"FAILED",0,errors);
        }
        for(int i=1;i<rows.size();i++) {
            List<String> row=rows.get(i); LocalDate date=row.get(5).isBlank()?null:LocalDate.parse(row.get(5));
            InvestigationCase record=new InvestigationCase(row.get(0).trim(),row.get(1).trim(),empty(row.get(8)),row.get(2).trim(),row.get(3),row.get(4),date,empty(row.get(7)),empty(row.get(6)),null,null,actor.getAgency(),null,actor);
            record.addTimeline(new CaseTimeline("Case imported","Imported from CSV job "+jobId,actor));
            cases.save(record);
        }
        cases.flush();
        int processed=rows.size()-1;
        persistJob(jobId,name,"SUCCEEDED",processed,actor);
        audit.record(actor,"INGESTION_SUCCEEDED","ingestion:"+jobId,"Imported "+processed+" case rows");
        return new IngestionResponse(jobId,"SUCCEEDED",processed,List.of());
    }

    private void persistJob(UUID id,String file,String status,int count,User actor) {
        jdbc.update("INSERT INTO ingestion_jobs(id,original_file_name,media_type,status,rows_processed,agency,created_by_id) VALUES(?,?,? ,?,?,?,?)",id,safeName(file),"text/csv",status,count,actor.getAgency(),actor.getId());
    }
    private List<List<String>> parse(String content) {
        if(content.indexOf('\0')>=0) throw new IllegalArgumentException("file is not UTF-8 text");
        List<List<String>> rows=new ArrayList<>(); List<String> row=new ArrayList<>(); StringBuilder cell=new StringBuilder(); boolean quoted=false;
        for(int i=0;i<content.length();i++) { char c=content.charAt(i);
            if(quoted) { if(c=='"' && i+1<content.length() && content.charAt(i+1)=='"') {cell.append('"');i++;} else if(c=='"') quoted=false; else cell.append(c); }
            else if(c=='"' && cell.isEmpty()) quoted=true;
            else if(c==',') {row.add(cell.toString());cell.setLength(0);}
            else if(c=='\n' || c=='\r') { if(c=='\r' && i+1<content.length() && content.charAt(i+1)=='\n') i++; row.add(cell.toString());cell.setLength(0); if(!(row.size()==1&&row.getFirst().isBlank())) rows.add(row); row=new ArrayList<>(); }
            else cell.append(c);
        }
        if(quoted) throw new IllegalArgumentException("unterminated quoted field");
        if(!row.isEmpty() || !cell.isEmpty()) {row.add(cell.toString());rows.add(row);}
        if(!rows.isEmpty() && rows.getFirst().getFirst().startsWith("\uFEFF")) { List<String> first=new ArrayList<>(rows.getFirst()); first.set(0,first.getFirst().substring(1)); rows.set(0,first); }
        return rows;
    }
    private String empty(String value) { return value.isBlank()?null:value.trim(); }
    private String safeName(String value) { String cleaned=value.replaceAll("[\\r\\n\\\\/]","_"); return cleaned.length()>255?cleaned.substring(cleaned.length()-255):cleaned; }
}
