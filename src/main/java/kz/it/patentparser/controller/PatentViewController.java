package kz.it.patentparser.controller;

import jakarta.servlet.http.HttpServletResponse;
import kz.it.patentparser.dto.DetailPatentDto;
import kz.it.patentparser.model.Patent;
import kz.it.patentparser.service.PatentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/patents")
public class PatentViewController {
    private final PatentService patentService;

    public PatentViewController(PatentService patentService) {
        this.patentService = patentService;
    }

    @GetMapping
    public String listAndSearchPatents(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String siteType,
            @RequestParam(required = false) Boolean expired,
            @RequestParam(defaultValue = "0") String category,
            @RequestParam(required = false) String mktu,
            @RequestParam(required = false) String securityDocNumber,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);

        boolean isSearchQuery = !query.isEmpty() || startDate != null || endDate != null || siteType != null || expired != null || !category.equals("0");
        Page<Patent> patentPage = isSearchQuery ?
                patentService.searchPatents(query, startDate, endDate, siteType, expired, category, mktu, securityDocNumber, pageable) :
                patentService.getPatents(page, size);

        model.addAttribute("query", query);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("siteType", siteType);
        model.addAttribute("expired", expired);
        model.addAttribute("category", category);
        model.addAttribute("mktu", mktu);
        model.addAttribute("securityDocNumber", securityDocNumber);
        model.addAttribute("patents", patentPage.getContent());
        model.addAttribute("totalElements", patentPage.getTotalElements());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", patentPage.getTotalPages());

        return "patents";
    }

    @GetMapping("/details/{id}")
    public ResponseEntity<DetailPatentDto> getPatentDetails(@PathVariable Long id) {
        return patentService.findById(id)
                .map(patent -> ResponseEntity.ok(DetailPatentDto.fromEntity(patent)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/export")
    public void exportToCsv(HttpServletResponse response,
                            @RequestParam(defaultValue = "") String query,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                            @RequestParam(required = false) String siteType,
                            @RequestParam(required = false) Boolean expired,
                            @RequestParam(defaultValue = "0") String category,
                            @RequestParam(required = false) String mktu,
                            @RequestParam(required = false) String securityDocNumber) throws IOException {
        Pageable pageable = PageRequest.of(1, 300000);

        boolean isSearchQuery = !query.isEmpty() || startDate != null || endDate != null || siteType != null || expired != null || !category.equals("0");
        List<Patent> patents = isSearchQuery ?
                patentService.searchPatents(query, startDate, endDate, siteType, expired, category, mktu, securityDocNumber, pageable).getContent() :
                patentService.getPatents(1, 300000).getContent();

        patentService.exportToCsv(response, patents);
    }
}
