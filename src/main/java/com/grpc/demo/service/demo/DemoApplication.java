package com.grpc.demo.service.demo;

import java.util.Scanner;
import java.util.concurrent.Executors;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.demo.grpc.studentservice.client.ServiceStudentClient;
import com.google.common.primitives.Ints;
import com.grpc.dao.student.service.Student;
import com.grpc.dao.student.service.StudentStatusRequest;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.CallStreamObserver;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;

@SpringBootApplication
public class DemoApplication {

	private static final ServiceStudentClient client = new ServiceStudentClient();
	private static final int DEFAULT_REQUESTS = 3;

	public static void main(String[] args) {
		runApplication();
	}

	private static void runApplication() {
		boolean keepRunning = true;

		System.out.println("###################################################");
		System.out.println("#       **** Welcome to demo ****                 #");
		System.out.println("#                                                 #");
		System.out.println("#                 GRPC java                       #");
		System.out.println("#                                                 #");
		System.out.println("#                                                 #");
		System.out.println("#             Location: Chernivtsi                #");
		System.out.println("#                                                 #");
		System.out.println("###################################################");

		while (keepRunning) {
			System.out.println("Choose a method for testing: ");
			System.out.println("1 - deadline propagation");
			System.out.println("2 - cancel propagation");
			System.out.println("3 - client flow control");
			Scanner scanner = new Scanner(System.in);

			switch (scanner.nextLine()) {
			case "1":
				testDeadLinePropagation();
				break;
			case "2":
				testCancelPropagation();
				break;
			case "3":
				testClientFlowControl();
			}
		}
	}

	private static void testDeadLinePropagation() {
		System.out.println("Choose deadline time in ms");
		Scanner scanner = new Scanner(System.in);
		try {
			System.out.println(client.getStudentByAge(32, Ints.tryParse(scanner.nextLine())));
		} catch (StatusRuntimeException sre) {
			System.out.println("!!! DEADLINE_EXCEEDED  !!!!!");
		}
	}

	private static void testCancelPropagation() {
		try {
			Executors.newSingleThreadExecutor().submit(() -> {
				client.getStudentByAge(32, 10000);
			});
			Thread.sleep(1000);
			System.exit(0);
		} catch (StatusRuntimeException sre) {
			System.out.println("!!! CANCELED !!!!!");
			System.out.println(sre.getStatus().getDescription());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static void testClientFlowControl() {

		ClientResponseObserver<StudentStatusRequest, Student> streamObserver = new ClientResponseObserver<StudentStatusRequest, Student>() {

			@Override public void beforeStart(ClientCallStreamObserver<StudentStatusRequest> outboundStream) {
				outboundStream.disableAutoInboundFlowControl();
			}

			@Override public void onNext(Student student) {
				System.out.println("Async client onNext: " + student);
			}

			@Override public void onError(Throwable throwable) {}

			@Override public void onCompleted() {
				System.out.println("Call completed!!!");
			}
		};

		CallStreamObserver<StudentStatusRequest> observer =
			(CallStreamObserver<StudentStatusRequest>) client.findStudentByStatusStream(streamObserver);

		observer.onNext(StudentStatusRequest.newBuilder()
				.setStatus(true)
				.build());

		observer.request(DEFAULT_REQUESTS);

		boolean keepRunning = true;

		Scanner scanner = new Scanner(System.in);

		while (keepRunning) {
			String input = scanner.nextLine();
			Integer requested = Ints.tryParse(input);
			if (requested != null) {
				System.out.printf("Requested %d responses. %n%n", requested);
				observer.request(requested);
			} else if ("q".equals(input)) {
				keepRunning = false;
			}
		}
	}

}
