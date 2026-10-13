package com.shiftschedule.security;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.shiftschedule.domain.Account;
import com.shiftschedule.domain.Period;
import com.shiftschedule.domain.Priority;
import com.shiftschedule.domain.RequestStatus;
import com.shiftschedule.domain.Role;
import com.shiftschedule.domain.ShiftRequest;
import com.shiftschedule.domain.ShiftRequestNote;
import com.shiftschedule.domain.WorkTask;
import com.shiftschedule.repository.AccountRepository;
import com.shiftschedule.repository.ShiftRequestNoteRepository;
import com.shiftschedule.repository.ShiftRequestRepository;
import com.shiftschedule.repository.WorkTaskRepository;

/**
 * 動作確認用のモックユーザー（モック01〜）と、来期の希望を作成する。
 * app.mock-data.count を設定したときだけ動き、ユーザー番号 20000001 から連番で作る。
 * 既に存在するユーザー番号は作り直さない。パスワードは全員 app.mock-data.password。
 */
@Component
@Order(3)
public class MockDataRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MockDataRunner.class);

	private static final long FIRST_USER_NUMBER = 20000001L;

	private final AccountRepository accounts;
	private final ShiftRequestRepository requests;
	private final ShiftRequestNoteRepository notes;
	private final WorkTaskRepository taskRepository;
	private final PasswordEncoder encoder;
	private final int count;
	private final String password;

	public MockDataRunner(AccountRepository accounts, ShiftRequestRepository requests,
			ShiftRequestNoteRepository notes, WorkTaskRepository taskRepository, PasswordEncoder encoder,
			@Value("${app.mock-data.count:0}") int count,
			@Value("${app.mock-data.password:password}") String password) {
		this.accounts = accounts;
		this.requests = requests;
		this.notes = notes;
		this.taskRepository = taskRepository;
		this.encoder = encoder;
		this.count = count;
		this.password = password;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (count <= 0) {
			return;
		}
		Period period = Period.upcoming();
		List<WorkTask> tasks = taskRepository.findAllByOrderByGroup_SortOrderAscGroup_IdAscSortOrderAscIdAsc();
		if (tasks.isEmpty()) {
			return;
		}
		int created = 0;
		for (int i = 1; i <= count; i++) {
			String userNumber = String.valueOf(FIRST_USER_NUMBER + i - 1);
			if (accounts.existsByUserNumber(userNumber)) {
				continue;
			}
			Random random = new Random(userNumber.hashCode());

			Account user = new Account();
			user.setUserNumber(userNumber);
			user.setPassword(encoder.encode(password));
			user.setName(String.format("モック%02d", i));
			user.setRole(Role.USER);
			user.setHireDate(LocalDate.now(Period.ZONE).minusMonths(1 + random.nextInt(36)));
			user.setPriority(Priority.values()[random.nextInt(Priority.values().length)]);
			Set<WorkTask> own = new HashSet<>(tasks.subList(0, Math.min(2, tasks.size())));
			for (int t = 0; t < 3; t++) {
				own.add(tasks.get(random.nextInt(tasks.size())));
			}
			user.setTasks(own);
			accounts.save(user);

			List<ShiftRequest> list = new ArrayList<>();
			for (LocalDate date : period.dates()) {
				int r = random.nextInt(10);
				RequestStatus status = r < 6 ? RequestStatus.AVAILABLE : r < 9 ? RequestStatus.OFF : RequestStatus.UNDECIDED;
				list.add(new ShiftRequest(user, date, status));
			}
			requests.saveAll(list);

			ShiftRequestNote note = new ShiftRequestNote(user, period.start());
			note.setUpdatedAt(LocalDateTime.now(Period.ZONE));
			notes.save(note);
			created++;
		}
		log.info("モックユーザーを {} 人作成しました（来期 {}）。", created, period.label());
	}
}
