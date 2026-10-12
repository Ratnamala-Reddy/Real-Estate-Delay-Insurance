import { TestBed } from '@angular/core/testing';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NotificationService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should add a toast message on success call', (done) => {
    service.success('Test Title', 'Test Message');
    service.toasts$.subscribe(toasts => {
      expect(toasts.length).toBe(1);
      expect(toasts[0].title).toBe('Test Title');
      expect(toasts[0].type).toBe('success');
      done();
    });
  });

  it('should remove toast message by id', (done) => {
    service.info('Info', 'Detail');
    service.toasts$.subscribe(toasts => {
      if (toasts.length > 0) {
        const id = toasts[0].id;
        service.remove(id);
      } else {
        expect(toasts.length).toBe(0);
        done();
      }
    });
  });
});
