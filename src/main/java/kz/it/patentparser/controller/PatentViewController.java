package kz.it.patentparser.controller;

import com.google.common.net.HttpHeaders;
import jakarta.servlet.http.HttpServletResponse;
import kz.it.patentparser.dto.DetailPatentDto;
import kz.it.patentparser.model.Patent;
import kz.it.patentparser.service.PatentService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
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
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String siteType,
            @RequestParam(required = false) Boolean expired,
            @RequestParam(defaultValue = "0") String category,
            @RequestParam(required = false) String mktu,
            @RequestParam(required = false) String securityDocNumber,
            Model model) {

        Page<Patent> patentPage;
        if (query.isEmpty() && startDate == null && endDate == null && siteType == null && expired == null && category.equals("0")) {
            // Если нет поисковых параметров → показываем все патенты
            patentPage = patentService.getPatents(page, size);
        } else {
            // Если есть параметры → выполняем поиск
            patentPage = patentService.searchPatents(query, startDate, endDate, page, size, siteType, expired, category, mktu, securityDocNumber);
        }

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
    public String exportToCsv(HttpServletResponse response,
                            @RequestParam(required = false, defaultValue = "") String query,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                            @RequestParam(required = false) String siteType,
                            @RequestParam(required = false) Boolean expired,
                            @RequestParam(defaultValue = "0") String category,
                            @RequestParam(required = false) String mktu,
                            @RequestParam(required = false) String securityDocNumber,
                              Model model) throws IOException {

        List<Patent> patents = new ArrayList<>();
        if (query.isEmpty() && startDate == null && endDate == null && siteType == null && expired == null && category.equals("0")) {
            // Если нет поисковых параметров → показываем все патенты
            patents = patentService.getPatents(1, 300000).getContent();
        } else {
            // Если есть параметры → выполняем поиск
            patents = patentService.searchPatents(query, startDate, endDate, 1, 100000, siteType, expired, category, mktu, securityDocNumber).getContent();
        }
        patentService.exportToCsv(response, patents);
        model.addAttribute("patents", patents);

        return "patents";
    }
}
