package com.recorday.recorday.frame.repository;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.TestPropertySource;

import com.recorday.recorday.auth.oauth2.enums.Provider;
import com.recorday.recorday.frame.entity.attributes.ColorBackgroundAttributes;
import com.recorday.recorday.frame.entity.Frame;
import com.recorday.recorday.frame.entity.FrameComponent;
import com.recorday.recorday.frame.enums.ComponentType;
import com.recorday.recorday.frame.enums.FrameType;
import com.recorday.recorday.user.entity.User;
import com.recorday.recorday.user.enums.UserRole;
import com.recorday.recorday.user.enums.UserStatus;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.yml")
class FrameRepositoryTest {

	@Autowired
	private FrameRepository frameRepository;

	@Autowired
	private TestEntityManager em;

	@TestConfiguration
	@EnableJpaAuditing
	static class TestConfig {
	}

	@Test
	@DisplayName("User ID를 기반으로 연관된 FrameComponent를 한 번의 쿼리로 삭제한다.")
	void deleteComponentsByUserIdTest() {
		//given
		User user = createUser("test@test.com");

		Frame frame1 = createFrame("f1", "desc1");

		user.addFrame(frame1);

		FrameComponent comp1 = FrameComponent.builder()
			.type(ComponentType.TEXT)
			.source("Happy Day") // 텍스트 내용
			.x(10).y(20).width(200.0).height(50.0) // 필수 좌표
			.rotation(0).zIndex(1)
			.build();

		FrameComponent comp2 = FrameComponent.builder()
			.type(ComponentType.PHOTO)
			.source("https://s3.../img.jpg") // 이미지 URL
			.x(50).y(60).width(300.0).height(400.0)
			.rotation(15).zIndex(2)
			.build();

		frame1.addComponent(comp1);
		frame1.addComponent(comp2);

		em.persist(user);
		em.flush();
		em.clear();

		//when
		frameRepository.deleteComponentsByUserId(user.getId());

		//then
		em.flush();
		em.clear();

		Frame findFrame1 = em.find(Frame.class, frame1.getId());

		assertThat(findFrame1).isNotNull();
		assertThat(findFrame1.getComponents()).isEmpty();
	}

	@Test
	@DisplayName("User ID를 기반으로 연관된 Frame을 한 번의 쿼리로 삭제한다.")
	void deleteFramesByUserIdTest() {
		// given
		User user = createUser("del@test.com");
		Frame frame = createFrame("delF", "delDesc");

		user.addFrame(frame);

		em.persist(user);
		em.flush();
		em.clear();

		// when
		frameRepository.deleteFramesByUserId(user.getId());

		// then
		User findUser = em.find(User.class, user.getId());
		assertThat(findUser.getFrames()).isEmpty();
	}

	private User createUser(String email) {
		return User.builder()
			.email(email)
			.username("tester")
			.profileUrl("http://profile.url")
			.provider(Provider.HARUCUT)
			.userRole(UserRole.ROLE_USER)
			.userStatus(UserStatus.ACTIVE)
			.build();
	}

	private Frame createFrame(String name, String description) {
		return Frame.builder()
			.title(name)
			.description(description)
			.previewKey("https://s3.../img.jpg")
			.frameType(FrameType.CLASSIC)
			.background(new ColorBackgroundAttributes("#FFFFFF"))
			.build();
	}

}