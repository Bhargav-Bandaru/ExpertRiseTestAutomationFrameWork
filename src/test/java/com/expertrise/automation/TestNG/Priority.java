package com.expertrise.automation.TestNG;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class Priority {
    @BeforeClass
    public void beforeClass() {
        System.out.println("This method runs before class");
    }
    @BeforeMethod
    public void beforeMethod() {
        System.out.println();
        System.out.println("This method runs before every method");
    }

    @Test(priority = 0)
    public void BMW3Series() {
        System.out.println("Running Test 1 - BMW 3 Series");
    }
    @Test(priority = 1)
    public void BMWX3() {
        System.out.println("Running Test 2 - BMW X3");
    }
    @Test(priority = 2, enabled = false)
    public void AudiA6() {
        System.out.println("Running Test 3 - Audi A6");
    }
    @Test(priority = 3)
    public void HondaAccord() {
        System.out.println("Running Test 4 - Honda Accord");
    }

    @AfterMethod
    public void afterMethod() {
        System.out.println("This method runs after every method");
    }

    @AfterClass
    public void afterClass() {
        System.out.println();
        System.out.println("This method runs after class");
        System.out.println();
    }

}
