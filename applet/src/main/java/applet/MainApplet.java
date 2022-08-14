package applet;

import javacard.framework.*;
import javacard.security.RandomData;

public class MainApplet extends Applet implements MultiSelectable
{
	public static final byte INS_ECHO = 0x00;
	public static final byte INS_RANDOM = 0x7E;

	public static final byte INS_GET_BALANCE = 0x02;
    public static final byte INS_CREDIT = 0x04;
    public static final byte INS_DEBIT = 0x06;

	public static final byte INS_GET_DATA = 0x08;
	public static final byte INS_PUT_DATA = 0x0A;

	public static final byte INS_GET_MEMSIZE = 0x10;

    public static final short MAX_BALANCE = 10000;
    public static final short MAX_CREDIT = 5000;
    public static final short MAX_DEBIT = 1000;

    private short balance;

	private static final short BUFFER_SIZE = 32;

	private byte[] tmpbuf = JCSystem.makeTransientByteArray(BUFFER_SIZE, JCSystem.CLEAR_ON_DESELECT);
	private RandomData random;

	private byte[] data = new byte[BUFFER_SIZE];

	private short[] membuf = JCSystem.makeTransientShortArray((short) 2, JCSystem.CLEAR_ON_DESELECT);

	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new MainApplet(bArray, bOffset, bLength);
	}
	
	protected MainApplet(byte[] buffer, short offset, byte length)
	{
		random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
		balance = 0;
		register();
	}

	// public void process(APDU apdu)
	// {
	// 	byte[] apduBuffer = apdu.getBuffer();
	// 	byte cla = apduBuffer[ISO7816.OFFSET_CLA];
	// 	byte ins = apduBuffer[ISO7816.OFFSET_INS];
	// 	short lc = (short)apduBuffer[ISO7816.OFFSET_LC];
	// 	short p1 = (short)apduBuffer[ISO7816.OFFSET_P1];
	// 	short p2 = (short)apduBuffer[ISO7816.OFFSET_P2];

	// 	random.generateData(tmpBuffer, (short) 0, BUFFER_SIZE);

	// 	Util.arrayCopyNonAtomic(tmpBuffer, (short)0, apduBuffer, (short)0, BUFFER_SIZE);
	// 	apdu.setOutgoingAndSend((short)0, BUFFER_SIZE);
	// }

	public boolean select(boolean b) {
		return true;
	}

	public void deselect(boolean b) {

	}

	public void process(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        if (selectingApplet()) return;

        switch (buffer[ISO7816.OFFSET_INS]) {
			case INS_ECHO:
                echo(apdu, buffer);
                return;
			case INS_RANDOM:
                random(apdu, buffer);
                return;
            case INS_GET_BALANCE:
                getBalance(apdu, buffer);
                return;
            case INS_CREDIT:
                credit(apdu, buffer);
                return;
            case INS_DEBIT:
                debit(apdu, buffer);
                return;
			case INS_GET_DATA:
                getData(apdu, buffer);
                return;
			case INS_PUT_DATA:
                setData(apdu, buffer);
                return;
			case INS_GET_MEMSIZE:
                getAvailableMemory(apdu, buffer);
                return;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

	private void echo(APDU apdu, byte[] buffer) {
        short length = apdu.setIncomingAndReceive();
        apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, (byte) length);
    }

	private void random(APDU apdu, byte[] buffer) {
		random.generateData(tmpbuf, (short) 0, BUFFER_SIZE);

		Util.arrayCopyNonAtomic(tmpbuf, (short)0, buffer, (short)0, BUFFER_SIZE);
		apdu.setOutgoingAndSend((short)0, BUFFER_SIZE);
    }

	private void getBalance(APDU apdu, byte[] buffer) {
        Util.setShort(buffer, (byte) 0, balance);
        apdu.setOutgoingAndSend((byte) 0, (byte) 2);
    }

    private void debit(APDU apdu, byte[] buffer) {
        short debit;

        if (apdu.setIncomingAndReceive() != 2)
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

        debit = Util.getShort(buffer, ISO7816.OFFSET_CDATA);

        if ((debit > balance) || (debit <= 0) ||
                (debit > MAX_DEBIT))
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);

        balance -= debit;

        getBalance(apdu, buffer);
    }

    private void credit(APDU apdu, byte[] buffer) {
        short credit;
        if (apdu.setIncomingAndReceive() != 2)
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        credit = Util.getShort(buffer, ISO7816.OFFSET_CDATA);

        if (((short) (credit + balance) > MAX_BALANCE) ||
                (credit <= 0) ||
                (credit > MAX_CREDIT))
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);

        balance += credit;

        getBalance(apdu, buffer);
    }

	private void getData(APDU apdu, byte[] buffer) {
		Util.arrayCopyNonAtomic(data, (short)0, buffer, (short)0, BUFFER_SIZE);
        apdu.setOutgoingAndSend((short)0, (byte) BUFFER_SIZE);
    }

	private void setData(APDU apdu, byte[] buffer) {
        short length = apdu.setIncomingAndReceive();

		Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, data, (short)0, length);
		apdu.setOutgoingAndSend(ISO7816.OFFSET_CDATA, length);
    }

	private void getAvailableMemory(APDU apdu, byte[] buffer) {
		JCSystem.getAvailableMemory(membuf, (short) 0,  JCSystem.MEMORY_TYPE_PERSISTENT);
		Util.setShort(buffer, (short) 0, membuf[0]);
		Util.setShort(buffer, (short) 2, membuf[1]);
        apdu.setOutgoingAndSend((short)0, (byte) 4);
    }
}
