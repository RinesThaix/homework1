package ru.ifmo.android_2016.calc;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author 0xC0deBabe <iam@kostya.sexy>
 */

public final class CalculatorActivity extends Activity implements View.OnClickListener {

    private final String RESULT_DEFAULT = "0.0";
    private final String ERROR = "?????";

    private final Map<Integer, Runnable> runnableComponents = new HashMap<>();

    private TextView result;

    private Operation selectedOperation = null;
    private String previous = "";
    private boolean waitingForAnotherArgument = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        result = (TextView) findViewById(R.id.result);
        if (savedInstanceState != null) {
            String s = savedInstanceState.getString("previous");
            previous = s;
            s = savedInstanceState.getString("operation");
            if(s != null)
                selectedOperation = Operation.valueOf(s);
            s = savedInstanceState.getString("result");
            if(s != null)
                result.setText(s);
        }
        result.setText(RESULT_DEFAULT);
        result.setMovementMethod(new ScrollingMovementMethod());
        preloadDigitButtons();
        preloadOperationButtons();
        preloadButton(R.id.clear, new Runnable() {
            @Override
            public void run() {
                previous = null;
                selectedOperation = null;
                result.setText(RESULT_DEFAULT);
            }
        });
        preloadButton(R.id.eqv, new Runnable() {
            @Override
            public void run() {
                calculateResult();
            }
        });
    }

    private void preloadOperationButtons() {
        Class<?> clazz = R.id.class;
        for(Operation op : Operation.values())
            try {
                Field f = clazz.getField(op.name().toLowerCase());
                preloadButton((int) f.get(null), new OperationSelectionRunnable(op));
            }catch(Exception ex) {
                ex.printStackTrace();
            }
    }

    private void preloadDigitButtons() {
        Class<?> clazz = R.id.class;
        for(int digit = 0; digit < 10; ++digit) {
            try {
                Field f = clazz.getField("d" + digit);
                preloadButton((int) f.get(null), new DigitChangeRunnable(digit));
            }catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void preloadButton(int id, Runnable runnable) {
        Button b = (Button) findViewById(id);
        b.setOnClickListener(this);
        runnableComponents.put(id, runnable);
    }

    private void appendDigit(int digit) {
        if(waitingForAnotherArgument) {
            waitingForAnotherArgument = false;
            previous = result.getText().toString();
            result.setText(digit + "");
            return;
        }
        String txt = result.getText().toString();
        if(txt.equals(ERROR) || new BigDecimal(txt).signum() == 0) {
            if(digit == 0)
                return;
            result.setText(digit + "");
        }else {
            result.setText(result.getText() + "" + digit);
        }
    }

    private void selectOperation(Operation op) {
        waitingForAnotherArgument = true;
        selectedOperation = op;
    }

    @Override
    public void onClick(View v) {
        Runnable runnable = runnableComponents.get(v.getId());
        if(runnable == null)
            return;
        runnable.run();
    }

    private void calculateResult() {
        if(previous == null || result.getText() == null || selectedOperation == null || previous.equals(ERROR))
            return;
        String current = result.getText().toString();
        if(current.equals(ERROR))
            return;
        BigDecimal bd = new BigDecimal(previous), cur = new BigDecimal(current);
        switch(selectedOperation) {
            case ADD:
                bd = bd.add(cur);
                break;
            case SUB:
                bd = bd.subtract(cur);
                break;
            case MUL:
                bd = bd.multiply(cur);
                break;
            case DIV:
                if(cur.signum() == 0) {
                    result.setText(ERROR);
                    return;
                }
                bd = bd.divide(cur, 5, RoundingMode.HALF_EVEN);
                break;
            default:
                break;
        }
        result.setText(bd.toString());
        previous = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("previous", previous);
        if(selectedOperation != null)
            outState.putString("operation", selectedOperation.name());
        if(result.getText() != null)
            outState.putString("result", result.getText().toString());
    }

    private class OperationSelectionRunnable implements Runnable {

        private final Operation operation;

        public OperationSelectionRunnable(Operation operation) {
            this.operation = operation;
        }

        @Override
        public void run() {
            selectOperation(this.operation);
        }
    }

    private class DigitChangeRunnable implements Runnable {

        private final int digit;

        public DigitChangeRunnable(int digit) {
            this.digit = digit;
        }

        @Override
        public void run() {
            appendDigit(this.digit);
        }
    }

    private enum Operation {
        ADD, SUB, MUL, DIV;
    }

}
