package com.soodfolio.api.config;

import com.soodfolio.api.entity.*;
import com.soodfolio.api.repository.PortfolioRepository;
import com.soodfolio.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
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

    @Override
    @Transactional
    public void run(String... args) {
        if (portfolioRepository.findByUsername("alex").isPresent()) {
            log.info("Sample portfolio 'alex' already exists. Skipping seed.");
            return;
        }

        log.info("Seeding initial portfolio 'alex'...");

        User user = User.builder()
                .email("alex@soodfolio.com")
                .fullName("Alex Nguyen")
                .passwordHash(passwordEncoder.encode("Admin123"))
                .role("ROLE_USER")
                .authProvider("LOCAL")
                .build();
        userRepository.save(user);

        Portfolio portfolio = Portfolio.builder()
                .user(user)
                .username("alex")
                .title("ALEX NGUYEN")
                .tagline("SENIOR FULLSTACK DEVELOPER")
                .bio("I design and build high-performance web applications with minimalist aesthetics, clean architecture, and delightful user interactions.")
                .statusText("AVAILABLE FOR WORK")
                .themeMode("DARK")
                .build();

        // Social Links
        portfolio.getSocialLinks().add(SocialLink.builder().portfolio(portfolio).platform("GITHUB").url("https://github.com/soodthin").label("[ GitHub ]").orderIndex(0).build());
        portfolio.getSocialLinks().add(SocialLink.builder().portfolio(portfolio).platform("LINKEDIN").url("https://linkedin.com").label("[ LinkedIn ]").orderIndex(1).build());
        portfolio.getSocialLinks().add(SocialLink.builder().portfolio(portfolio).platform("EMAIL").url("mailto:alex@example.com").label("[ Email ]").orderIndex(2).build());

        // Experiences
        portfolio.getExperiences().add(Experience.builder()
                .portfolio(portfolio)
                .roleTitle("Senior Fullstack Engineer")
                .companyName("TechCorp Innovations")
                .companyUrl("https://techcorp.example.com")
                .location("Ho Chi Minh City, Vietnam")
                .periodText("2024 — PRESENT")
                .description("Architecting and building scalable microservices and presentation-first web applications.")
                .highlights(List.of(
                        "Designed event-driven backend reducing API latency by 45%",
                        "Led migration of legacy monolith to Spring Boot 3 & Next.js App Router",
                        "Mentored 6 junior and mid-level engineers"
                ))
                .skillsUsed(List.of("Spring Boot 3", "Java 17", "PostgreSQL", "Next.js", "Docker"))
                .orderIndex(0)
                .build());

        // Projects
        portfolio.getProjects().add(Project.builder()
                .portfolio(portfolio)
                .title("FlexiConnect")
                .subtitle("Enterprise B2B Integration Platform")
                .role("Lead Architect")
                .periodText("2024")
                .description("A high-throughput API gateway and event routing system connecting 30+ enterprise ERPs.")
                .highlights(List.of("Processed 10M+ daily events with 99.99% uptime", "Implemented automated retry and dead-letter queues"))
                .techStack(List.of("Java 17", "Spring Boot", "Kafka", "PostgreSQL", "Redis"))
                .liveUrl("https://flexiconnect.demo")
                .githubUrl("https://github.com/soodthin/flexiconnect")
                .orderIndex(0)
                .build());

        portfolio.getProjects().add(Project.builder()
                .portfolio(portfolio)
                .title("SoodFolio Platform")
                .subtitle("Slide-Deck Portfolio SaaS for Developers")
                .role("Creator & Fullstack Developer")
                .periodText("2026")
                .description("Interactive presentation-style portfolio builder empowering tech professionals to showcase their work.")
                .highlights(List.of("Minimalist typographic design with zero emojis/icons", "Multi-tenant routing with instant static generation"))
                .techStack(List.of("Next.js 16", "Spring Boot 3", "Tailwind CSS", "PostgreSQL"))
                .liveUrl("https://soodfolio.vercel.app")
                .githubUrl("https://github.com/soodthin/soodfolio-web")
                .orderIndex(1)
                .build());

        // Skills
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("BACKEND").name("Java 17/21").orderIndex(0).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("BACKEND").name("Spring Boot 3").orderIndex(1).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("BACKEND").name("Spring Security").orderIndex(2).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("FRONTEND").name("TypeScript").orderIndex(3).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("FRONTEND").name("Next.js 16").orderIndex(4).build());
        portfolio.getSkills().add(Skill.builder().portfolio(portfolio).category("DATABASE").name("PostgreSQL").orderIndex(5).build());

        // Education
        portfolio.getEducations().add(Education.builder()
                .portfolio(portfolio)
                .institution("University of Technology")
                .degree("Bachelor of Science")
                .fieldOfStudy("Computer Science")
                .periodText("2020 — 2024")
                .highlights("Graduated with Honors. Specialization in Distributed Systems & Software Architecture.")
                .orderIndex(0)
                .build());

        portfolioRepository.save(portfolio);
        log.info("Sample portfolio 'alex' seeded successfully!");
    }
}
