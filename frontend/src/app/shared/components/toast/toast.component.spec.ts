import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToastComponent } from './toast.component';
import { NotificationService } from '../../../core/services/notification.service';

describe('ToastComponent', () => {
  let component: ToastComponent;
  let fixture: ComponentFixture<ToastComponent>;
  let notificationService: NotificationService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToastComponent],
      providers: [NotificationService]
    }).compileComponents();

    notificationService = TestBed.inject(NotificationService);
    fixture = TestBed.createComponent(ToastComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create toast component', () => {
    expect(component).toBeTruthy();
  });

  it('should trigger close', () => {
    spyOn(notificationService, 'remove');
    component.close('123');
    expect(notificationService.remove).toHaveBeenCalledWith('123');
  });
});
