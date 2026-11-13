package com.soodfolio.api.config;

import com.soodfolio.api.entity.*;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment env;

    @Override
    @Transactional
    public void run(String... args) {
        seedSoodthin();
        seedDemo();
        seedAdmin();
    }

    private void seedSoodthin() {
        if (portfolioRepository.findByUsername("soodthin").isPresent()) {
            log.info("Portfolio 'soodthin' already exists. Skipping seed.");
            return;
        }

        log.info("Seeding authentic portfolio 'soodthin' (Thai Do Thinh)...");

        User user = userRepository.findByEmail("thinhthai963@gmail.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("thinhthai963@gmail.com")
                        .fullName("Thai Do Thinh")
                        .passwordHash(passwordEncoder.encode("Admin123"))
                        .role("ROLE_USER")
                        .authProvider("LOCAL")
                        .avatarUrl("/images/avatar.jpg")
                        .build()));

        Portfolio portfolio = createThaiDoThinhPortfolio(user, "soodthin");
        portfolioRepository.save(portfolio);
        log.info("Portfolio 'soodthin' seeded successfully!");
    }

    private void seedDemo() {
        if (portfolioRepository.findByUsername("demo").isPresent()) {
            log.info("Portfolio 'demo' already exists. Skipping seed.");
            return;
        }

        log.info("Seeding demo portfolio 'demo'...");

        boolean isDevOrLocal = env.acceptsProfiles(Profiles.of("dev", "local"));
        String passwordHash = isDevOrLocal ? passwordEncoder.encode("Admin123") : null;

        User user = userRepository.findByEmail("demo@soodfolio.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("demo@soodfolio.com")
                        .fullName("Thai Do Thinh (Demo)")
                        .passwordHash(passwordHash)
                        .role("ROLE_USER")
                        .authProvider("LOCAL")
                        .avatarUrl("/images/avatar.jpg")
                        .build()));

        Portfolio portfolio = createThaiDoThinhPortfolio(user, "demo");
        portfolioRepository.save(portfolio);
        log.info("Portfolio 'demo' seeded successfully (passwordHash={})!", isDevOrLocal ? "SET" : "NULL");
    }

    private void seedAdmin() {
        boolean isDevOrLocal = env.acceptsProfiles(Profiles.of("dev", "local"));
        if (!isDevOrLocal) {
            return;
        }

        if (userRepository.findByEmail("admin@soodfolio.dev").isPresent()) {
            return;
        }

        log.info("Seeding local admin user 'admin@soodfolio.dev'...");
        User admin = User.builder()
                .email("admin@soodfolio.dev")
                .fullName("Platform Administrator")
                .passwordHash(passwordEncoder.encode("Admin123"))
                .role("ROLE_ADMIN")
                .authProvider("LOCAL")
                .build();
        userRepository.save(admin);
        log.info("Admin user seeded successfully!");
    }

    private Portfolio createThaiDoThinhPortfolio(User user, String username) {
        Portfolio portfolio = Portfolio.builder()
                .user(user)
                .username(username)
                .title("THAI DO THINH")
                .tagline("SOFTWARE ENGINEER · FULL-STACK DEVELOPER")
                .bio("Final-year Computer Science student at Ho Chi Minh City Open University who has built and deployed several real web products, from database design to frontend and backend. Comfortable with Java, Python, JavaScript, TypeScript, C++, and frameworks like React and Spring Boot. I combine solid fundamental engineering with modern AI-assisted workflows to deliver high quality, reliable software quickly.")
                .statusText("AVAILABLE FOR DEVELOPER ROLES // READY TO DEPLOY")
                .resumeUrl("#transmit")
                .themeMode("DARK")
                .isPublished(true)
                .build();

        // Social Links
        portfolio.getSocialLinks().add(SocialLink.builder().portfolio(portfolio).platform("LINKEDIN").url("https://linkedin.com/in/thinhthai").label("LinkedIn: in/thinhthai").orderIndex(0).build());
        portfolio.getSocialLinks().add(SocialLink.builder().portfolio(portfolio).platform("GITHUB").url("https://github.com/soodthin").label("GitHub: soodthin").orderIndex(1).build());
        portfolio.getSocialLinks().add(SocialLink.builder().portfolio(portfolio).platform("EMAIL").url("mailto:thinhthai963@gmail.com").label("thinhthai963@gmail.com").orderIndex(2).build());
        portfolio.getSocialLinks().add(SocialLink.builder().portfolio(portfolio).platform("PHONE").url("tel:0869922096").label("Tel: 0869922096").orderIndex(3).build());

        // Experience: BW Industrial
        portfolio.getExperiences().add(Experience.builder()
                .portfolio(portfolio)
                .roleTitle("Finance AI & Automation Intern")
                .companyName("BW Industrial Development Joint Stock Company")
                .companyUrl("https://bwid-automation.onrender.com/")
                .location("Ho Chi Minh City, Viet Nam")
                .periodText("Oct 2025 — Apr 2026")
                .description("Engineered an internal web application used by the finance and accounting team to process bank statements and generate automated cash reports, handling transaction extraction, classification, and reconciliation.")
                .highlights(List.of(
                        "Bank Statement Parsing: Built REST APIs with FastAPI, including parsers for 10+ banks (VCB, TCB, BIDV, ACB, MBB...) to automatically extract transaction data from uploaded statements, with OCR fallback via Gemini for scanned files.",
                        "Cash Report Automation: Built a session-based pipeline to process and reconcile cash reports from multiple uploaded files, including automated transaction classification based on rules learned from the finance team's feedback — cutting processing time from about a day to two hours.",
                        "Security & Access Control: Implemented enterprise-grade authentication with Google OAuth2 SSO (restricted to company email domain), JWT access tokens with refresh-token session tracking across devices, plus role-based access control (RBAC) separating admin and finance-team permissions.",
                        "Frontend Integration: Built the UI with React, TypeScript, and Tailwind CSS for both workflows, including a review/classification screen and a real-time progress tracker (via SSE) for long-running file processing.",
                        "Reliability & Monitoring: Set up centralized structured logging and health-check endpoints, plus an internal admin dashboard tracking AI usage cost, active sessions, and processing stats in real time.",
                        "Documentation: Wrote step-by-step usage guides with screenshots so the finance team could use both tools on their own."
                ))
                .skillsUsed(List.of("React", "TypeScript", "Tailwind CSS", "FastAPI", "PostgreSQL", "Redis", "Google OAuth2", "SSE"))
                .orderIndex(0)
                .build());

        // Projects
        portfolio.getProjects().add(Project.builder()
                .portfolio(portfolio)
                .title("FlexiConnect Web")
                .subtitle("Job Portal & Application Tracker Platform")
                .role("Lead Fullstack Developer")
                .periodText("2025")
                .description("Full-featured recruitment platform connecting candidates, employers, and administrators with comprehensive role-based access, job lifecycle management, and application workflows.")
                .highlights(List.of(
                        "Backend & Database: Built REST APIs with Spring Boot and Spring Data JPA on a MySQL database, handling job listings, applications, and role-based access for candidates, employers, and admin.",
                        "Security: Implemented authentication and authorization with Spring Security and JWT.",
                        "Frontend Integration: Built the UI with ReactJS, Vite, and Tailwind CSS, consuming the backend REST API across all three user roles.",
                        "Deployment: Deployed on Render, resolving build errors independently to keep the app stable."
                ))
                .techStack(List.of("Spring Boot", "ReactJS", "Vite", "Tailwind CSS", "MySQL", "Spring Security", "JWT", "Render"))
                .imageUrl("/images/flexiconnect1.png")
                .githubUrl("https://github.com/soodthin/FlexiConnect")
                .orderIndex(0)
                .build());

        portfolio.getProjects().add(Project.builder()
                .portfolio(portfolio)
                .title("Food Delivery & Restaurant Booking App")
                .subtitle("Fullstack Mobile Experience with Dual-Role Portals")
                .role("Fullstack Mobile Engineer")
                .periodText("2025")
                .description("Cross-platform mobile application covering customer food delivery ordering, table reservations, restaurant menu management, and dedicated kitchen staff (chef) workflows.")
                .highlights(List.of(
                        "Backend & Database: Built REST APIs with Django REST Framework, covering menus, table bookings, orders, and reviews.",
                        "Payment Integration: Integrated MoMo and Stripe for order payments, including webhook handling for payment confirmation.",
                        "Frontend Integration: Built the mobile UI with React Native and Expo, covering separate flows for customers and kitchen staff (chefs).",
                        "Real-time Features: Implemented in-app chat between customers and the restaurant using Firebase."
                ))
                .techStack(List.of("React Native", "Expo", "Django", "Django REST Framework", "MoMo", "Stripe", "Firebase"))
                .imageUrl("/images/restaurant1.png")
                .githubUrl("https://github.com/soodthin/T-Restaurant-App")
                .orderIndex(1)
                .build());

        portfolio.getProjects().add(Project.builder()
                .portfolio(portfolio)
                .title("Dev Portfolio Engine")
                .subtitle("Pure Typographic Static Portfolio Architecture")
                .role("Creator & Frontend Architect")
                .periodText("2026")
                .description("Minimalist, high-performance portfolio engine built with Next.js App Router and TypeScript, compiled to static export for zero-latency hosting on GitHub Pages via automated CI/CD.")
                .highlights(List.of(
                        "100% static export (SSG) with automated deployment pipeline via GitHub Actions.",
                        "Pure typographic 2D vector flat-layered design system with zero icon or emoji dependencies.",
                        "Strict TypeScript typing throughout data models and component interfaces."
                ))
                .techStack(List.of("Next.js 16", "TypeScript", "Tailwind CSS v4", "GitHub Pages", "GitHub Actions"))
                .liveUrl("https://soodthin.github.io/dev-portfolio/")
                .githubUrl("https://github.com/soodthin/dev-portfolio")
                .orderIndex(2)
                .build());

        // Skills
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("LANGUAGES").name("JavaScript").orderIndex(0).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("LANGUAGES").name("TypeScript").orderIndex(1).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("LANGUAGES").name("Java").orderIndex(2).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("LANGUAGES").name("Python").orderIndex(3).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("LANGUAGES").name("C++").orderIndex(4).build());

        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("FRAMEWORKS").name("ReactJS").orderIndex(5).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("FRAMEWORKS").name("Next.js").orderIndex(6).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("FRAMEWORKS").name("Spring Boot").orderIndex(7).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("FRAMEWORKS").name("FastAPI").orderIndex(8).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("FRAMEWORKS").name("Django").orderIndex(9).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("FRAMEWORKS").name("Tailwind CSS").orderIndex(10).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("FRAMEWORKS").name("RESTful API").orderIndex(11).build());

        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("DATABASES").name("PostgreSQL").orderIndex(12).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("DATABASES").name("MySQL").orderIndex(13).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("DATABASES").name("Redis").orderIndex(14).build());

        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("DEVOPS").name("Git").orderIndex(15).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("DEVOPS").name("GitHub Actions").orderIndex(16).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("DEVOPS").name("Postman").orderIndex(17).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("DEVOPS").name("Render").orderIndex(18).build());

        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("AI_ENGINEERING").name("Claude Code").orderIndex(19).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("AI_ENGINEERING").name("Codex").orderIndex(20).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("AI_ENGINEERING").name("Antigravity (IDE)").orderIndex(21).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("AI_ENGINEERING").name("Prompt Engineering").orderIndex(22).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("AI_ENGINEERING").name("Code Review & Verification").orderIndex(23).build());

        // Education
        portfolio.getEducations().add(Education.builder()
                .portfolio(portfolio)
                .institution("Ho Chi Minh City Open University (HCMCOU)")
                .degree("Bachelor of Science in Computer Science")
                .fieldOfStudy("Computer Science")
                .periodText("Oct 2022 — Present")
                .highlights("Coursework: Software Engineering, Web System Development, Object-Oriented Programming (OOP), Data Structures & Algorithms, Modern Programming Technologies, Database Management Systems, Cloud Computing.")
                .orderIndex(0)
                .build());

        return portfolio;
    }
}
