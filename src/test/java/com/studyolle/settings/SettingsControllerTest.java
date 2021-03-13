package com.studyolle.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyolle.WithAccount;
import com.studyolle.account.AccountRepository;
import com.studyolle.account.AccountService;
import com.studyolle.account.SignUpForm;
import com.studyolle.domain.Account;
import com.studyolle.domain.Tag;
import com.studyolle.settings.form.TagForm;
import com.studyolle.tag.TagRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.studyolle.settings.SettingsController.*;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class SettingsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AccountService accountService;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TagRepository tagRepository;

    @BeforeEach
    void beforeEach() {
        SignUpForm signUpForm = new SignUpForm();
        signUpForm.setNickname("gicheolWithUserDetails");
        signUpForm.setEmail("gicheol@a.com");
        signUpForm.setPassword("12345678");
        accountService.processNewAccount(signUpForm);
    }

    @AfterEach
    void afterEach() {
        accountRepository.deleteAll();
    }

    @WithAccount("gicheol")
    @DisplayName("프로필 수정 폼")
    @Test
    void updateProfileForm() throws Exception {
        String bio = "짧은 소개를 수정하는 경우.";
        mockMvc.perform(get(ROOT + SETTINGS + PROFILE))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("profile"))
        ;
    }

    @WithAccount("gicheolWithAccount")
    @DisplayName("프로필 수정하기 - 입력값 정상 (방법 1 : @WithAccount 정의해서 사용. 이 경우 AfterEach 필수)")
    @Test
    void updateProfile1() throws Exception {
        String bio = "짧은 소개를 수정하는 경우.";
        mockMvc.perform(post(ROOT + SETTINGS + PROFILE)
                .param("bio", bio)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(ROOT + SETTINGS + PROFILE))
                .andExpect(flash().attributeExists("message"))
        ;

        Account gicheol = accountRepository.findByNickname("gicheolWithAccount");
        assertEquals(bio, gicheol.getBio());
    }

    @WithUserDetails(value = "gicheolWithUserDetails", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("프로필 수정하기 - 입력값 정상 (방법 2 : @WithUserDetails 사용)")
    @Test
    void updateProfile2() throws Exception {
        String bio = "짧은 소개를 수정하는 경우.";
        mockMvc.perform(post(ROOT + SETTINGS + PROFILE)
                .param("bio", bio)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(ROOT + SETTINGS + PROFILE))
                .andExpect(flash().attributeExists("message"))
        ;

        Account gicheol = accountRepository.findByNickname("gicheolWithUserDetails");
        assertEquals(bio, gicheol.getBio());
    }



    @WithAccount("gicheolError")
    @DisplayName("프로필 수정하기 - 입력값 에러")
    @Test
    void updateProfile_error() throws Exception {
        String bio = "길게 소개를 수정하는 경우. 길게 소개를 수정하는 경우. 길게 소개를 수정하는 경우. 길게 소개를 수정하는 경우. 길게 소개를 수정하는 경우. 길게 소개를 수정하는 경우. 길게 소개를 수정하는 경우.";
        mockMvc.perform(post(ROOT + SETTINGS + PROFILE)
                .param("bio", bio)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(SETTINGS + PROFILE))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("profile"))
                .andExpect(model().hasErrors())
        ;

        Account gicheol = accountRepository.findByNickname("gicheolError");
        assertNull(gicheol.getBio());
    }

    @WithAccount("gicheol")
    @DisplayName("패스워드 수정 폼")
    @Test
    void updatePassword_form() throws Exception {
        mockMvc.perform(get(ROOT + SETTINGS + PASSWORD))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("passwordForm"))
        ;
    }

    @WithAccount("gicheol")
    @DisplayName("패스워드 수정 - 입력값 정상")
    void updatePassword_success() throws Exception {
        mockMvc.perform(post(ROOT + SETTINGS + PASSWORD)
                    .param("newPassword", "12345678")
                    .param("newPasswordConfirm", "12345678")
                    .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(ROOT + SETTINGS + PASSWORD))
                .andExpect(flash().attributeExists("message"))
        ;

        Account gicheol = accountRepository.findByNickname("gicheol");
        assertTrue(passwordEncoder.matches("12345678", gicheol.getPassword()));
    }

    @WithAccount("gicheol")
    @DisplayName("패스워드 수정 - 입력 값 에러 - 패스워드 불일치")
    @Test
    void updatePassword_fail() throws Exception {
        mockMvc.perform(post(ROOT + SETTINGS + PASSWORD)
                        .param("newPassword", "12345678")
                        .param("newPasswordConfirm", "11111111")
                        .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name(SETTINGS + PASSWORD))
                    .andExpect(model().hasErrors())
                    .andExpect(model().attributeExists("passwordForm"))
                    .andExpect(model().attributeExists("account"))
        ;
    }

    @WithAccount("gicheol")
    @DisplayName("계정의 태그 수정 폼")
    @Test
    void updateTagsForm() throws Exception {
        mockMvc.perform(get(ROOT + SETTINGS + TAGS))
                .andExpect(view().name(SETTINGS + TAGS))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("whitelist"))
                .andExpect(model().attributeExists("tags"))
        ;
    }

    @WithAccount("gicheol")
    @DisplayName("계정의 태그 추가")
    @Test
    void addTag() throws Exception {
        TagForm tagForm = new TagForm();
        tagForm.setTagTitle("newTag");

        mockMvc.perform(post(ROOT + SETTINGS + TAGS + "/add")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(tagForm))
                    .with(csrf()))
                .andExpect(status().isOk())
        ;

        Tag newTag = tagRepository.findByTitle("newTag");

        assertNotNull(newTag);
        Account gicheol = accountRepository.findByNickname("gicheol");
        assertTrue(gicheol.getTags().contains(newTag));
    }

    @WithAccount("gicheol")
    @DisplayName("계정의 태그 삭제")
    @Test
    void removeTag() throws Exception {
        Account gicheol = accountRepository.findByNickname("gicheol");
        Tag newTag = tagRepository.save(Tag.builder().title("newTag").build());
        accountService.addTag(gicheol, newTag);

        assertTrue(gicheol.getTags().contains(newTag));

        TagForm tagForm = new TagForm();
        tagForm.setTagTitle("newTag");

        mockMvc.perform(post(ROOT + SETTINGS + TAGS + "/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tagForm))
                        .with(csrf()))
                    .andExpect(status().isOk())
        ;

        assertFalse(gicheol.getTags().contains(newTag));
    }

}